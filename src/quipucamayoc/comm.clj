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
  (if (>= val 220)
    (do (println val)
        220)
    (if (<= val 20)
      (do (println val)
          20)
      val)))

(defn map-midi [val]
  (map-range val 20 220 22 43))

(defn map-sad [val]
  (map-range val 22 43 1.0 7.0))

(defn by-bean [vals]
  (let [spl (partition 3 vals)]
    (into [] (map (fn [[a b c]] {:a a :b b :c c :fa (map-sad a) :fb (map-sad b) :fc (map-sad c)}) spl))))

;         (case (count vals)
;           3   {:a (first vals) :b (second vals) :c (last vals)  :fa (map-sad (first vals)) :fb (map-sad (second vals)) :fc (map-sad (last vals)) :fd (map-sad (last vals))}
;           6   {:a (first vals) :b (nth vals 3)  :c (second vals) :fa (map-sad (first vals)) :fb (map-sad (nth vals 3)) :fc (map-sad (second vals)) :fd (map-sad (nth vals 5))}
;           9   {:a (first vals) :b (nth vals 3)  :c (nth vals 6)  :fa (map-sad (first vals)) :fb  (map-sad (nth vals 3)) :fc  (map-sad (nth vals 6)) :fd  (map-sad (nth vals 5))}
;           12  {:a (first vals) :b (nth vals 3)  :c (nth vals 6)  :fa (map-sad (/ (+ (second vals) (nth vals 2)) 2)) :fb (map-sad (/ (+ (nth vals 3) (nth vals 4)) 2)) :fc (map-sad (/ (+ (nth vals 6) (nth vals 7)) 2)) :fd (map-sad (/ (+ (nth vals 10) (nth vals 11)) 2))}))
;
;{:inst1 {:a :b :c :fa :fb :fc}
; :inst2 {:a :b :c :fa :fb :fc}
; :inst3 {:a :b :c :fa :fb :fc}
; :inst4 {:a :b :c :fa :fb :fc}}

(defn adjust-tone []
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
    #_(println "vals: " vals)
    (when (not-empty vals)
      (go (>! iot-stream {:topic :inc-pitch-by
                          :msg (by-bean vals)})))))

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
    #_(comment at/every 2000 #(osc-heartbeat) heart-pool)))

