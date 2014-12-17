(ns quipucamayoc.tone
  (:require [clojure.core.async :as a :refer [<! go-loop]]
            [quipucamayoc.comm :as comm]
            [quil.core :refer [map-range]]
            [overtone.core :refer :all]
            [overtone.synth.stringed :refer :all]
            [overtone.synth.sts :refer :all]
            [overtone.at-at :as at]))

(defonce sc-server (boot-internal-server))

(def lg-guitar (guitar))
(def md-guitar (guitar))
(def sm-guitar (guitar))

(defn playat [time delta offset]
  (+ time (* offset delta)))

;; Axis based guitar trigger.

(defmulti axis-trigger :action)

(defmethod axis-trigger :large [msg]
  (case (:sensor msg)
    :x (let [time (now) delta 281]
         (guitar-pick lg-guitar 1 3 (playat time delta 0))
         (guitar-pick lg-guitar 1 -1 (playat time delta 5)))
    :y (let [time (now) delta 282]
         (guitar-pick md-guitar 1 8 (playat time delta 0))
         (guitar-pick md-guitar 1 -1 (playat time delta 6)))
    :z (let [time (now) delta 283]
         (guitar-pick sm-guitar 1 10 (playat time delta 0))
         (guitar-pick sm-guitar 1 -1 (playat time delta 8)))))

(defmethod axis-trigger :medium [msg]
  (case (:sensor msg)
    :x (let [time (now) delta 291]
         (guitar-pick lg-guitar 3 3 (playat time delta 0))
         (guitar-pick md-guitar 3 -1 (playat time delta 6)))
    :y (let [time (now) delta 292]
         (guitar-pick lg-guitar 3 8 (playat time delta 0))
         (guitar-pick md-guitar 3 -1 (playat time delta 5)))
    :z (let [time (now) delta 293]
         (guitar-pick sm-guitar 3 10 (playat time delta 0))
         (guitar-pick sm-guitar 3 -1 (playat time delta 8)))))

(defmethod axis-trigger :small [msg]
  (case (:sensor msg)
    :x (let [time (now) delta 301]
         (guitar-pick lg-guitar 5 3 (playat time delta 0))
         (guitar-pick lg-guitar 5 -1 (playat time delta 8)))
    :y (let [time (now) delta 302]
         (guitar-pick md-guitar 5 8 (playat time delta 0))
         (guitar-pick md-guitar 5 -1 (playat time delta 6)))
    :z (let [time (now) delta 303]
         (guitar-pick sm-guitar 5 10 (playat time delta 0))
         (guitar-pick sm-guitar 5 -1 (playat time delta 5)))))

;; Event distributor

(defmulti control :topic)

(defmethod control :axis-trigger [{:keys [msg]}]
  (axis-trigger msg)
  (println "Got" msg))

(defn init []
  (println "Init Sound Server")

  (ctl lg-guitar :pre-amp 4.3 :distort 0.41
       :lp-freq 3000 :lp-rq 0.12
       :rvb-mix 0.1 :rvb-room 0.7 :rvb-damp 0.2)

  (ctl md-guitar :pre-amp 4.2 :distort 0.31
       :lp-freq 2500 :lp-rq 0.2
       :rvb-mix 0.2 :rvb-room 0.7 :rvb-damp 0.5)

  (ctl sm-guitar :pre-amp 4.0 :distort 0.31
       :lp-freq 2000 :lp-rq 0.12
       :rvb-mix 0.1 :rvb-room 0.7 :rvb-damp 0.2)

  (go-loop []
    (when-let [v (<! comm/adjust-tone)]
      (control v)
      (recur))))