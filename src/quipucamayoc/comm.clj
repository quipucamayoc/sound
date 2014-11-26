(ns quipucamayoc.comm
  (:require [overtone.osc :as o :refer [osc-server osc-client osc-handle osc-send zero-conf-on]]
            [clojure.core.async :as async :refer [dropping-buffer sub chan pub go go-loop <! >! put! <!! >!! timeout]]
            ;[cassiel.zeroconf.client :as cl]
            ;[cassiel.zeroconf.server :as s]
            ;[cognitect.transit :as t] ;; Look Into
            [quil.core :as q] ;; Only using math, replace.
            [clojure.math.numeric-tower :as math]
            [clojure.pprint :refer [pprint]]))


(declare adjust adjust-bean adjust-tone)

;(def server (osc-server 9800))
;
;;(osc-handle server "/heartbeat" (fn [msg] (println "MSG: " msg)))
;(osc-handle server "/beans" #(adjust %))
;
;(def client (osc-client "192.168.0.141" 9800))
;
;(defn osc-heartbeat []
;  (osc-send client "/my-addr"  (.getHostAddress (java.net.InetAddress/getLocalHost))))

(def iot-stream (chan (dropping-buffer 50)))

(def sub-to-iot
  (pub iot-stream #(:topic %)))

(def adjust-tone-with (chan))

(sub sub-to-iot :inc-pitch-by adjust-tone-with)
(sub sub-to-iot :plain-inst adjust-tone-with)

(defn adjust [msg]
  (let [args (into [] (:args msg))]
    ;;(pprint (into [] (:args msg)))
    (adjust-bean args)
    (adjust-tone args)))

(defn constrain [v u l]
  (let [val (cond
              (> v u) u
              (< v l) l
              :else v)]
    val))

(defn adjust-tone [[id x y z]]
  (let [nota (q/map-range (math/floor (* 100 (math/abs x))) 0 1000 55 25)
        notb (q/map-range (math/floor (* 100 (math/abs y))) 0 1000 55 25)
        notc (q/map-range (math/floor (* 100 (math/abs z))) 0 1000 55 25)]
  (go (>! iot-stream {:topic :inc-pitch-by
                      :msg {:nota      nota
                            :notb      notb
                            :notc      notc
                            :fa        (q/map-range x -2 2 0.01 0.99)
                            :fb   (q/map-range y -2 2 0.01 0.99)
                            :fc  (q/map-range z -2 2 0.01 0.99)}}))))


(defn test-sound []
  (let [nota [25 30 35 41 43 47 49 51 56]
        notb [25 30 35 41 43 47 49 51 56]
        notc [25 30 35 41 43 47 49 51 56]
        f [0.1 0.2 0.3 0.4 0.5 0.6 0.7 0.8 0.9]]
    (go (>! iot-stream {:topic :inc-pitch-by
                        :msg {:nota (rand-nth nota)
                              :notb (rand-nth notb)
                              :notc (rand-nth notc)
                              :fa (rand-nth f)
                              :fb (rand-nth f)
                              :fc (rand-nth f)}}))))

;;(go (>! publisher { :topic :bean-map :username "billy" }))

;(def PORT 9800)
;(def server (osc-server 9800 "quipu"))
;(zero-conf-on)

;;(doseq [val (range 10)]
;;  (osc-send client "/test" "1"))

;;(osc-listen server (fn [msg] (println "Listener: " msg)) :debug)

