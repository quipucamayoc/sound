(ns quipucamayoc.comm
  (:import (java.net InetAddress))
  (:require [overtone.osc :as o :refer [osc-server osc-client osc-handle osc-send zero-conf-on]]
            [clojure.core.async :as async :refer [dropping-buffer sub chan pub go go-loop <! >! put! <!! >!! timeout]]
            [quil.core :refer [map-range round abs]]
            [clojure.pprint :refer [pprint]]
            [clojure.core.typed :as t]
            [schema.core :as s]))

; #Data integrity validation

(def beans-merged
  "A schema for merged-beans"
  {s/Keyword {s/Keyword [s/Num]}})

(declare adjust adjust-tone bean-watcher)

; #Async Works

;; ##Main Chan
(def iot-stream (chan (dropping-buffer 1024)))

(def sub-to-iot
  (pub iot-stream #(:topic %)))

;; ##Sound Events
(def adjust-tone (chan))
(sub sub-to-iot :raw-write  adjust-tone)
(sub sub-to-iot :axis-trigger  adjust-tone)

;; ##IO Events
(def adjust-data (chan))
(def watch-events (chan))
(sub sub-to-iot :bean-input adjust-data)
(sub sub-to-iot :bean-hist  watch-events)

; #Bean Input and History

(def bean-input (ref {}))
(def bean-history (atom []))

(defn start-hist-watcher
  "Watches the input `ref` and triggers `bean-watcher` on update."
  []
  (add-watch bean-input :historian bean-watcher))

(defn update-bean
  "Consumes the data sent from the Bean server. Stores it in a common ref."
  [msg]
  (let [id (keyword (first msg))
        axis (keyword (second msg))
        value (last msg)]
    (dosync (alter bean-input #(merge-with merge % {id {axis value}})))))

(defn start-bean-listner
  "Listens to server inputs and calls adjust based on the messages recieved."
  []
  (go-loop []
    (when-let [v (<! adjust-data)]
      (update-bean (:msg v))
      (recur))))

;; # Command Logic
;; ## Analysis functions

(s/defn inc-check
  "Returns a positive or negative number based on the intensity of axis tilt."
  ([vals :- [s/Num]]
    (inc-check vals 0 0))
  ([vals :- [s/Num]
    movement :- s/Num
    past-val :- s/Num]
    (if (empty? vals)
      movement
      (recur (rest vals)
             (cond (> (first vals) past-val) (- past-val (first vals))
                   (< (first vals) past-val) (- past-val (first vals))
                   (== (first vals) past-val) movement
                   :else (do (println "Inc-Check issue with:" movement vals past-val)
                             movement))
             (first vals)))))

;; ## Per-Bean Triggers

(defn axis-differences
  "Detects and dispatches movements based on rise and fall"
  [lone-bean]
  (let [sensors (second lone-bean)
        {:keys [x y z]} sensors
        checked {:x (if x (inc-check x) 0)
                 :y (if y (inc-check y) 0)
                 :z (if z (inc-check z) 0)}
        checked-average (/ (apply + (vals checked)) 3)]
    ;(if (<= 6 checked-average)
    ;  (println "Trigger an all axis rise" checked-average))
    (dorun (map #(let [change (abs (- (abs (second %)) checked-average))]
                  (when-let [event-scale (cond (> change 150):large
                                               (> change 75) :medium
                                               (> change 25) :small)]
                    (go (>! iot-stream {:topic :axis-trigger
                                        :msg {:device (first lone-bean)
                                              :action event-scale
                                              :sensor (first %)}}))))
                checked))))

;; ## Organization

(defn arrange-bean-data [beans]
  (let [inter (apply merge-with concat beans)
        mediate (zipmap (keys inter) (map #(group-by first %) (vals inter)))
        merged-beans (into {}
                           (for [[k v] mediate]
                             [k (into {}
                                      (for [[k v] v]
                                        [k (vec (map second v))]))]))]
    merged-beans))

(defn adjust-instruments
  "Cleans up the over time data from the watcher and sends off commands based on
  calculated events"
  [beans]
  (when-let [merged-beans (s/validate beans-merged (arrange-bean-data beans))]
    (dorun (map
      #(-> %
           axis-differences)
      merged-beans))))

(defn start-watch-listner []
  (go-loop []
    (when-let [v (<! watch-events)]
      (adjust-instruments (:msg v))
      (recur))))

#_(go (>! iot-stream {:topic :raw-write
                      :msg (by-bean vals)}))

(defn bean-watcher
  "Stores a batch of previous bean states in order to access their historical data once
  the atom is full."
  [key bean-ref old-state new-state]
  (let [window 10
        beans @bean-history
        num-beans (count beans)]
    (if (<= window num-beans)
      (do
        (reset! bean-history [new-state])
        (go (>! iot-stream {:topic :bean-hist
                            :msg beans})))
      (if (not= old-state new-state)
        (swap! bean-history conj new-state)))))

;;; To move or remove

(defn cap [val]
  (if (>= val 220)
    220
    (if (<= val 20)
      20
      val)))

(defn map-midi [val]
  (map-range val 20 220 22 43))

(defn map-sad [val]
  (map-range val 22 43 1.0 7.0))

; #Server

(def client-remote (osc-client "192.168.0.141" 9800))
(def client-remote-b (osc-client "192.168.1.134" 9800))

(defn osc-heartbeat
  "Sends a heartbeat to the Bean server in order to share IP. Should be replaced with zero conf."
  []
  (let [ip (.getHostAddress (InetAddress/getLocalHost))]
    (try (osc-send client-remote-b "/my-addr" ip)
         (catch Exception e "no b"))
    (try (osc-send client-remote "/my-addr" ip)
         (catch Exception e "no r"))))

; #Launch Communication

(defn init[]
  (let [server (osc-server 9801)]
    (osc-handle server "/beans" (fn [msg]
                                  (let [data (:args msg)]
                                    (go (>! iot-stream {:topic :bean-input
                                                        :msg data})))))
    (osc-handle server "/heartbeat" (fn [msg]
                                      (println "MSG: " (:args msg))))
    (start-hist-watcher)
    (start-bean-listner)
    (start-watch-listner)))

