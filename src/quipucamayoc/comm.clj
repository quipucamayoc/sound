;; #Communication
;; Handles communication with the Wearable Devices server as well as passes all messages within the program.
;;
(ns quipucamayoc.comm
  (:import (java.net InetAddress))
  (:require [overtone.osc :as o :refer [osc-server osc-client osc-handle osc-send zero-conf-on]]
            [clojure.core.async :as async :refer [dropping-buffer sub chan pub go go-loop <! >! put! <!! >!! timeout]]
            [quil.core :refer [map-range round abs]]
            [clojure.pprint :refer [pprint]]))

(declare adjust adjust-tone device-watcher)

;; ##Async Works

;; ###Main Chan
(def iot-stream (chan (dropping-buffer 1024)))

;; ###Subscriber
(def sub-to-iot
  (pub iot-stream #(:topic %)))

;; ###Sound Events
(def adjust-tone (chan))
(sub sub-to-iot :raw-write  adjust-tone)
(sub sub-to-iot :axis-trigger  adjust-tone)

;; ###IO Events
(def adjust-data (chan))
(def watch-events (chan))
(sub sub-to-iot :device-input adjust-data)
(sub sub-to-iot :device-hist  watch-events)

;; ##Device Input and History
(def device-input (ref {}))
(def device-history (atom []))

(defn start-hist-watcher
  "Watches the input `ref` and triggers `device-watcher` on update."
  []
  (add-watch device-input :historian device-watcher))

(defn update-device
  "Consumes the data sent from the Device server. Stores it in a common ref."
  [msg]
  (let [id (keyword (first msg))
        axis (keyword (second msg))
        value (last msg)]
    (dosync (alter device-input #(merge-with merge % {id {axis value}})))))

(defn start-device-listner
  "Listens to server inputs and calls adjust based on the messages recieved."
  []
  (go-loop []
    (when-let [v (<! adjust-data)]
      (update-device (:msg v))
      (recur))))

;; ## Command Logic
;; ### Analysis functions

(defn inc-check
  "Returns a positive or negative number based on the intensity of axis tilt."
  ([vals]
    (inc-check vals 0 0))
  ([vals movement past-val]
    (if (empty? vals)
      movement
      (recur (rest vals)
             (cond (> (first vals) past-val) (- past-val (first vals))
                   (< (first vals) past-val) (- past-val (first vals))
                   (== (first vals) past-val) movement
                   :else (do (println "Inc-Check issue with:" movement vals past-val)
                             movement))
             (first vals)))))

;; ### Per-device Triggers

(defn axis-differences
  "Detects and dispatches movements based on rise and fall"
  [lone-device]
  (let [sensors (second lone-device)
        {:keys [x y z]} sensors
        checked {:x (if x (inc-check x) 0)
                 :y (if y (inc-check y) 0)
                 :z (if z (inc-check z) 0)}
        checked-average (/ (apply + (vals checked)) 3)]
    ;(if (<= 6 checked-average)
    ;  (println "Trigger an all axis rise" checked-average))
    (dorun (map #(let [change (abs (- (abs (second %)) checked-average))]
                  (when-let [event-scale (cond (> change 250) :large
                                               (> change 150) :medium
                                               (> change 35) :small)]
                    (go (>! iot-stream {:topic :axis-trigger
                                        :msg {:device (first lone-device)
                                              :action event-scale
                                              :sensor (first %)}}))))
                checked))))

;; ### Organization

(defn arrange-device-data
  "Moves the sensor data out from individual maps for each sensor tick into one map with keys for each sensor."
  [devices]
  (let [inter (apply merge-with concat devices)
        mediate (zipmap (keys inter) (map #(group-by first %) (vals inter)))
        merged-devices (into {}
                           (for [[k v] mediate]
                             [k (into {}
                                      (for [[k v] v]
                                        [k (vec (map second v))]))]))]
    merged-devices))

(defn adjust-instruments
  "Cleans up the over time data from the watcher and sends off commands based on
  calculated events"
  [devices]
  (when-let [merged-devices (arrange-device-data devices)]
    (dorun (map
      #(-> %
           axis-differences)
      merged-devices))))

(defn start-watch-listner
  "Async event passing from the watcher"
  []
  (go-loop []
    (when-let [v (<! watch-events)]
      (adjust-instruments (:msg v))
      (recur))))

(defn device-watcher
  "Stores a batch of previous device states in order to access their historical data once
  the atom is full."
  [key device-ref old-state new-state]
  (let [devices @device-history
        num-devices (count @device-input)
        history-states (count devices)
        window (* 5 num-devices)]
    (if (<= window history-states)
      (do
        (reset! device-history [new-state])
        (go (>! iot-stream {:topic :device-hist
                            :msg devices})))
      (if (not= old-state new-state)
        (swap! device-history conj new-state)))))

;; ##Server

(def client-remote (osc-client "192.168.0.141" 9800))
(def client-remote-b (osc-client "192.168.1.134" 9800))

(defn osc-heartbeat
  "Sends a heartbeat to the Device server in order to share IP. Should be replaced with zero conf."
  []
  (let [ip (.getHostAddress (InetAddress/getLocalHost))]
    (try (osc-send client-remote-b "/my-addr" ip)
         (catch Exception e "no b"))
    (try (osc-send client-remote "/my-addr" ip)
         (catch Exception e "no r"))))

;; ##Launch Communication

(defn init
  "Starts to listen to Open Sound Control server events. Initializes watchers and listners"
  []
  (let [server (osc-server 9801)]
    (osc-handle server "/beans" (fn [msg]
                                  (let [data (:args msg)]
                                    (go (>! iot-stream {:topic :device-input
                                                        :msg data})))))
    (osc-handle server "/heartbeat" (fn [msg]
                                      (println "MSG: " (:args msg))))
    (start-hist-watcher)
    (start-device-listner)
    (start-watch-listner)))

