(ns quipucamayoc.comm
  (:import (java.net InetAddress))
  (:require [overtone.osc :as o :refer [osc-server osc-client osc-handle osc-send zero-conf-on]]
            [clojure.core.async :as async :refer [dropping-buffer sub chan pub go go-loop <! >! put! <!! >!! timeout]]
            ;[cassiel.zeroconf.client :as cl]
            ;[cassiel.zeroconf.server :as s]
            ;[cognitect.transit :as t]
            ;; Look Into
            [quil.core :refer [map-range round]]
            ;; Only using math, replace.
            [clojure.math.numeric-tower :as math]
            [clojure.pprint :refer [pprint]]
            [overtone.at-at :as at]))


(declare adjust adjust-tone)

;(def heart-pool (at/mk-pool))

(def beans (atom {}))

(def iot-stream (chan (dropping-buffer 50)))

(def sub-to-iot
  (pub iot-stream #(:topic %)))

(def adjust-tone-with (chan))

(sub sub-to-iot :inc-pitch-by adjust-tone-with)
(sub sub-to-iot :plain-inst adjust-tone-with)

(defn adjust [msg]
  (let [id (keyword (first msg))
        axis (keyword (second msg))
        value (last msg)
        new-beans (merge-with merge @beans {id {axis value}})]
    (reset! beans new-beans)))

(defn cap [val]
  (if (>= val 300)
    300
    val))

(defn map-midi [val]
  (map-range val 0 300 26 39))

(defn adjust-tone []
  (comment case (count @beans)
    0 (println "No Beans!")
    1 (println "One Bean!")
    2 (println "Two Beans!")
    3 (println "Three Beans!")
    4 (println "All the beans!")
    :else (println "Empty!"))
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
    (println "vals: " vals)
    (when (not-empty vals)
      (go (>! iot-stream {:topic :inc-pitch-by
                          :msg vals})))))

(def client-remote (osc-client "192.168.0.141" 9800))
(def client-remote-b (osc-client "192.168.1.134" 9800))

(defn osc-heartbeat []
  (let [ip (.getHostAddress (InetAddress/getLocalHost))]
    (try (osc-send client-remote-b "/my-addr" ip)
         (catch Exception e "no b"))
    (try (osc-send client-remote "/my-addr" ip)
         (catch Exception e "no r"))))

(defn init[]
  (let [server (osc-server 9801)]
    (println server)
    (osc-handle server "/beans" (fn [msg]
                                  (let [data (:args msg)]
                                    (adjust data))))
    (osc-handle server "/heartbeat" (fn [msg]
                                      (println "MSG: " (:args msg))))
    ;(comment at/every 2000 #(osc-heartbeat) heart-pool)
    ))

