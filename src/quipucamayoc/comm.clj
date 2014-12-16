(ns quipucamayoc.comm
  (:import (java.net InetAddress))
  (:require [overtone.osc :as o :refer [osc-server osc-client osc-handle osc-send zero-conf-on]]
            [clojure.core.async :as async :refer [dropping-buffer sub chan pub go go-loop <! >! put! <!! >!! timeout]]
            [quil.core :refer [map-range round]]
            [clojure.pprint :refer [pprint]]))

(declare adjust adjust-tone bean-watcher)

;; Bean Input and History

(def bean-input (ref {}))
(def bean-history (atom []))

(defn start-bean-watcher
  "Watches the input `ref` and triggers `bean-watcher` on update."
  []
  (add-watch bean-input :historian bean-watcher))

(defn adjust
  "Consumes the data sent from the Bean server. Stores it in a common ref."
  [msg]
  (let [id (keyword (first msg))
        axis (keyword (second msg))
        value (last msg)]
    (dosync (alter bean-input #(merge-with merge % {id {axis value}})))))

;; Async Works

(def iot-stream (chan (dropping-buffer 1024)))

(def sub-to-iot
  (pub iot-stream #(:topic %)))

(def adjust-tone-with (chan))

(sub sub-to-iot :inc-pitch-by adjust-tone-with)
(sub sub-to-iot :plain-inst adjust-tone-with)

;; Command Logic

(defn adjust-instruments
  "Cleans up the over time data from the watcher and sends off commands based on
  calculated events"
  [beans]
  (let [inter (apply merge-with concat beans)
        mediate (zipmap (keys inter) (map #(group-by first %) (vals inter)))
        merged-beans (into {}
                           (for [[k v] mediate]
                             [k (into {}
                                      (for [[k v] v]
                                        [k (vec (map second v))]))]))]
    ;; Command Distribution.
    ))

(defn bean-watcher
  "Stores a batch of previous bean states in order to access their historical data once
  the atom is full."
  [key bean-ref old-state new-state]
  (let [window 20
        beans @bean-history
        num-beans (count beans)]
    (if (<= window num-beans)
      (do
        (reset! bean-history [new-state])
        (adjust-instruments beans))
      (if (not= old-state new-state)
        (swap! bean-history conj new-state)))))

;; Other

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

(defn by-bean [vals]
  (let [spl (partition 3 vals)]
    (into [] (map (fn [[a b c]] {:a a :b b :c c :fa (map-sad a) :fb (map-sad b) :fc (map-sad c)}) spl))))

#_(defn adjust-tone []
  (let [vals (->> (map
                    (fn [s]
                         (case (count s)
                            1 (conj
                                (map #(-> %
                                          (cap)
                                          (map-midi))
                                     s) 42 42)
                            2 (conj
                                (map #(-> %
                                          (cap)
                                          (map-midi))
                                     s) 42)
                            3 (map #(-> %
                                        (cap)
                                        (map-midi))
                                   s)))
                       (map (fn [m] (map val m)) (map val @beans)))
                  (apply concat)
                  (map round)
                  (into []))]
    (when (not-empty vals)
      (go (>! iot-stream {:topic :inc-pitch-by
                          :msg (by-bean vals)})))))

;; Server

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

;; Launch Communication

(defn init[]
  (let [server (osc-server 9801)]
    (start-bean-watcher)
    (osc-handle server "/beans" (fn [msg]
                                  (let [data (:args msg)]
                                    (adjust data))))
    (osc-handle server "/heartbeat" (fn [msg]
                                      (println "MSG: " (:args msg))))))

