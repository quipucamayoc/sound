(ns quipucamayoc.tone
  (:require [clojure.core.async :as a :refer [<! go-loop]]
            [quipucamayoc.comm :as comm]
            [quil.core :refer [map-range]]
            [overtone.core :refer :all]
            [overtone.synth.stringed :refer :all]
            [overtone.synth.sts :refer :all]
            [overtone.at-at :as at]))

(defonce sc-server (boot-internal-server))

(def sg (guitar))

(defn playat [time delta offset]
  (+ time (* offset delta)))

;; Axis based guitar.

(defmulti axis-trigger :action)

(defmethod axis-trigger :large [msg]
  (case (:sensor msg)
    :x (let [time (now) delta 281]
         (guitar-pick sg 1 3 (playat time delta 0))
         (guitar-pick sg 1 -1 (playat time delta 5)))
    :y (let [time (now) delta 282]
         (guitar-pick sg 1 8 (playat time delta 0))
         (guitar-pick sg 1 -1 (playat time delta 6)))
    :z (let [time (now) delta 283]
         (guitar-pick sg 1 10 (playat time delta 0))
         (guitar-pick sg 1 -1 (playat time delta 8)))))

(defmethod axis-trigger :medium [msg]
  (case (:sensor msg)
    :x (let [time (now) delta 291]
         (guitar-pick sg 3 3 (playat time delta 0))
         (guitar-pick sg 3 -1 (playat time delta 6)))
    :y (let [time (now) delta 292]
         (guitar-pick sg 3 8 (playat time delta 0))
         (guitar-pick sg 3 -1 (playat time delta 5)))
    :z (let [time (now) delta 293]
         (guitar-pick sg 3 10 (playat time delta 0))
         (guitar-pick sg 3 -1 (playat time delta 8)))))

(defmethod axis-trigger :small [msg]
  (case (:sensor msg)
    :x (let [time (now) delta 301]
         (guitar-pick sg 5 3 (playat time delta 0))
         (guitar-pick sg 5 -1 (playat time delta 8)))
    :y (let [time (now) delta 302]
         (guitar-pick sg 5 8 (playat time delta 0))
         (guitar-pick sg 5 -1 (playat time delta 6)))
    :z (let [time (now) delta 303]
         (guitar-pick sg 5 10 (playat time delta 0))
         (guitar-pick sg 5 -1 (playat time delta 5)))))

;; Event distributor

(defmulti control :topic)

(defmethod control :axis-trigger [{:keys [msg]}]
  (axis-trigger msg)
  (println "Got" msg))

(defn init []
  (println "Init Sound Server")

  (ctl sg :pre-amp 4.0 :distort 0.31
       :lp-freq 2000 :lp-rq 0.12
       :rvb-mix 0.1 :rvb-room 0.7 :rvb-damp 0.2)

  (go-loop []
    (when-let [v (<! comm/adjust-tone)]
      (control v)
      (recur))))