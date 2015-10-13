;; #Communication
;; Handles communication with the Wearable Devices server as well as passes all messages within the program.
;;
(ns sound.comm
  (:import (java.net InetAddress))
  (:require [overtone.osc :as o :refer [osc-server osc-client osc-handle osc-send zero-conf-on]]
            [clojure.core.async :as async :refer [dropping-buffer sub chan pub go go-loop <! >! put! <!! >!! timeout]]
            [quil.core :refer [map-range round abs norm constrain lerp floor]]
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
(sub sub-to-iot :raw-write adjust-tone)
(sub sub-to-iot :axis-trigger adjust-tone)
(sub sub-to-iot :sample-blend adjust-tone)
(sub sub-to-iot :change-inst adjust-tone)

;; ###IO Events
(def adjust-data (chan))
(def watch-events (chan))
(sub sub-to-iot :device-input adjust-data)
(sub sub-to-iot :device-hist watch-events)

;; ##Device Input and History
(def device-input (ref {}))
(def device-history (atom []))

(def colour-ranges
  (atom {:01 {:r [255 255] :g [255 255] :b [0 50]}
         :02 {:r [255 255] :g [255 255] :b [51 100]}
         :03 {:r [255 255] :g [255 255] :b [101 150]}
         :04 {:r [225 225] :g [225 225] :b [151 200]}
         :05 {:r [200 200] :g [200 200] :b [201 255]}

         :06 {:r [255 255] :g [0 50] :b [255 255]}
         :07 {:r [255 255] :g [51 100] :b [255 255]}
         :08 {:r [255 255] :g [101 150] :b [255 255]}
         :09 {:r [225 225] :g [151 200] :b [225 225]}
         :10 {:r [200 200] :g [201 255] :b [200 200]}

         :11 {:r [0 50] :g [255 255] :b [255 255]}
         :12 {:r [51 100] :g [255 255] :b [255 255]}
         :13 {:r [101 150] :g [255 255] :b [255 255]}
         :14 {:r [151 200] :g [225 225] :b [225 225]}
         :15 {:r [201 255] :g [200 200] :b [200 200]}

         :16 {:r [0 50] :g [0 50] :b [255 255]}
         :17 {:r [51 100] :g [51 100] :b [255 255]}
         :18 {:r [101 150] :g [101 150] :b [255 255]}
         :19 {:r [151 200] :g [151 200] :b [225 225]}
         :20 {:r [201 255] :g [201 255] :b [200 200]}

         :21 {:r [255 255] :g [0 50] :b [0 50]}
         :22 {:r [255 255] :g [51 100] :b [51 100]}
         :24 {:r [255 255] :g [101 150] :b [101 150]}
         :25 {:r [225 225] :g [151 200] :b [151 200]}
         :26 {:r [200 200] :g [201 255] :b [201 255]}

         :27 {:r [0 50] :g [255 255] :b [0 50]}
         :29 {:r [51 100] :g [255 255] :b [51 100]}
         :30 {:r [101 150] :g [255 255] :b [101 150]}
         :31 {:r [151 200] :g [225 225] :b [151 200]}
         :32 {:r [201 255] :g [200 200] :b [201 255]}

         :33 {:r [0 50] :g [0 50] :b [201 255]}
         :34 {:r [51 100] :g [51 100] :b [151 200]}
         :35 {:r [101 150] :g [101 150] :b [101 150]}
         :36 {:r [151 200] :g [151 200] :b [51 100]}
         :37 {:r [201 255] :g [201 255] :b [0 50]}

        :38 {:r [201 255] :g [0 50] :b [201 255]}
        :39 {:r [151 200] :g [51 100] :b [151 200]}
        :40 {:r [101 150] :g [101 150] :b [101 150]}
        :41 {:r [51 100] :g [151 200] :b [51 100]}
        :42 {:r [0 50] :g [201 255] :b [0 50]}}))

(def colour-assignments
  (atom {}))

(defn start-hist-watcher
  "Watches the input `ref` and triggers `device-watcher` on update."
  []
  (add-watch device-input :historian device-watcher))

(defn digits [n dig-max]
  (let [a (int-array dig-max)]
    (loop [n (int n) i (int (dec dig-max))]
      (when-not (zero? n)
        (aset a i (unchecked-remainder-int n 10))
        (recur
          (unchecked-divide-int n 10)
          (unchecked-dec-int i))))
    (seq a)))

(defn take-piece [first? item]
  (let [items (digits item 8)
        chunk (if first?
                (take 4 items)
                (drop 4 items))]
    (case (first chunk)
      1 (let [s (clojure.string/join "" (drop-while zero? (take-last 3 chunk)))]
          (if (= "" s)
            0
            (Integer/parseInt s)))
      2 (let [s (clojure.string/join "" (drop-while zero? (take-last 3 chunk)))]
          (if (= "" s)
            0
          (* (Integer/parseInt  s) -1)))
      3 false
      false)))

(defn constrain-axis [data min max]
    (map-range (constrain data -240 240) -240 240 min max))

(defn get-in-range [data {:keys [r g b]}]
  {:r (map-range (constrain data 0 255) 0 255 (first r) (second r))
   :g (map-range (constrain data 0 255) 0 255 (first g) (second g))
   :b (map-range (constrain data 0 255) 0 255 (first b) (second b))})

(defn get-range []
  (let [k (rand-nth (keys @colour-ranges))
        range (k @colour-ranges)]
    (swap! colour-ranges dissoc k)
    range))

(defn constrain-colour [id data]
  (when-not (contains? @colour-assignments id)
    (swap! colour-assignments merge {id (get-range)}))
  (if (false? data)
    (do (pprint "False Data?")
        {:r 0 :g 0 :b 0})
    (get-in-range data (id @colour-assignments))))

(defn constrain-weight [data]
  (map-range (constrain data 0 255) 0 255 2 42))



(defn alter-device-input [id k v]
  (dosync (alter device-input #(merge-with merge % {id {k v}}))))

(defn update-device
  "Consumes the data sent from the Device server. Stores it in a common ref."
  [msg]
  (let [[ID NAME SLOT value] msg
        id (keyword ID)
        name (keyword NAME)
        slot (floor SLOT)]

    (case slot
      1 (alter-device-input id :x value)
      2 (alter-device-input id :y value)
      3 (alter-device-input id :z value)
      4 (alter-device-input id :a value)
      5 (alter-device-input id :b value)
      6 (alter-device-input id :c value)
      7 (alter-device-input id :d value)
      8 (alter-device-input id :type value))
    #_(cond
      (= :x-y axis) (do
                     (dosync (alter device-input #(merge-with merge % {id {:x (constrain-axis (take-piece true value) 427 1047)}})))
                     (dosync (alter device-input #(merge-with merge % {id {:y (constrain-axis (take-piece false value) 283 623)}}))))
      (= :z-color axis) (do
                          (dosync (alter device-input #(merge-with merge % {id {:z (constrain-axis (take-piece true value) 236 576)}})))
                          (dosync (alter device-input #(merge-with merge % {id {:colour (constrain-colour id (take-piece false value))}}))))
      (= :weight-trail axis) (do
                               (dosync (alter device-input #(merge-with merge % {id {:weight (constrain-weight (take-piece true value))}})))
                               (dosync (alter device-input #(merge-with merge % {id {:trail (take-piece false value)}}))))
      (= :character-local axis) (do
                                  (dosync (alter device-input #(merge-with merge % {id {:character (take-piece true value)}})))
                                  (dosync (alter device-input #(merge-with merge % {id {:local (take-piece false value)}}))))
      :else nil)))

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
                  (when-let [event-scale (cond (> change 300) :large
                                               (> change 200) :medium
                                               (> change 50) :small)]
                    (go (>! iot-stream {:topic :axis-trigger
                                        :msg   {:device (first lone-device)
                                                :action event-scale
                                                :sensor (first %)}}))))
                checked))))

;; ### Provides control based on averaged and mapped axis data.

(defn create-volume [v in-min in-max]
  (if v
    (-> (/ (apply + v) (count v))
        (constrain in-min in-max)
        (map-range in-min in-max 0 250)
        (norm 0 250))
    0))

(defn send-tap [device tap scale variation]
  (if (< 2 (count tap))
    (when (= 1 (floor (last tap)))
      (go (>! iot-stream {:topic :axis-trigger
                          :msg   {:device device
                                  :action scale
                                  :sensor variation}})))))

(defn change-inst [device tap]
  (if (< 2 (count tap))
    (when (= 1 (floor (last tap)))
      (go (>! iot-stream {:topic :change-inst
                          :msg   {:device device
                                  :action :change}})))))

(defn axis-mapped
  "Detects and dispatches movements based on rise and fall"
  [lone-device in-min in-max topic action]
  (let [{:keys [x y z  a b c d]} (second lone-device)
        normalized [:vola (create-volume x in-min in-max)
                    :volb (create-volume y in-min in-max)
                    :volc (create-volume z in-min in-max)]]

    (if a (send-tap (first lone-device) a :small :x))
    (if b (send-tap (first lone-device) b :small :y))
    (if c (send-tap (first lone-device) c :small :z))
    (if d (send-tap (first lone-device) d :medium :x))

    #_(go (>! iot-stream {:topic topic
                        :msg   {:device (first lone-device)
                                :action action
                                :data   normalized}}))))

(defn axis-mapped-no-touch
  "Detects and dispatches movements based on rise and fall"
  [lone-device in-min in-max topic action]
  (let [{:keys [x y z  a b c d]} (second lone-device)
        normalized [:vola (create-volume x in-min in-max)
                    :volb (create-volume y in-min in-max)
                    :volc (create-volume z in-min in-max)]]

    (go (>! iot-stream {:topic topic
                        :msg   {:device (first lone-device)
                                :action action
                                :data   normalized}}))))

(defn axis-mapped-no-touch-upper-sensor
  "Detects and dispatches movements based on rise and fall"
  [lone-device in-min in-max topic action]
  (let [{:keys [x y z  a b c d]} (second lone-device)
        normalized [:vola (create-volume a in-min in-max)
                    :volb (create-volume b in-min in-max)
                    :volc (create-volume c in-min in-max)]]

    (go (>! iot-stream {:topic topic
                        :msg   {:device (first lone-device)
                                :action action
                                :data   normalized}}))))


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



    ;; Temporary setup for a per-instrument test.
    #_(println "hi")
    ;(let [vmap (vec merged-devices)]
    ;  (not= (nil? (first vmap))
    ;        (axis-mapped (first vmap) -250 250 :sample-blend :thunder-storm)))

    (let [vmap (vec merged-devices)
          num (count vmap)]
      (doall (mapv (fn [[id data]]
              (case (abs (last (:type data)))
                0 (axis-mapped-no-touch {id data} -250 250 :sample-blend :thunder-storm)
                1 (axis-mapped-no-touch-upper-sensor {id data} -250 250 :sample-blend :thunder-storm)
                2 (axis-mapped {id data} -250 250 :sample-blend :thunder-storm)
                (pprint data))) vmap)))

    (comment (fn [& args]
               (not= (nil? (first args))
                     (axis-differences (first args)))
               (not= (nil? (second args))
                     (axis-mapped (second args) 0 250 :sample-blend :thunder-storm))
               (not= (nil? (nth args 2))
                     (axis-mapped (nth args 2) 0 250 :sample-blend :fly-fire))
               (not= (nil? (nth args 3))
                     (axis-mapped (nth args 3) 0 250 :sample-blend :fly-wind)))
             (vals merged-devices))

    (comment dorun (map

                     (fn [lone-bean]
                       (case (first lone-bean)
                         :key1 (axis-differences lone-bean)
                         :key2 (axis-mapped lone-bean 0 250 :sample-blend :thunder-storm)
                         :key3 (axis-mapped lone-bean 0 250 :sample-blend :fly-fire)
                         (axis-differences lone-bean)))

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
                            :msg   devices})))
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
  (let [server (osc-server 41104)]
    (osc-handle server "/devices" (fn [msg]
                                  (let [data (:args msg)]
                                    (go (>! iot-stream {:topic :device-input
                                                        :msg   data}))
                                    #_(pprint data))))
    (osc-handle server "/heartbeat" (fn [msg]
                                      (println "MSG: " (:args msg))))
    (start-hist-watcher)
    (start-device-listner)
    (start-watch-listner)))