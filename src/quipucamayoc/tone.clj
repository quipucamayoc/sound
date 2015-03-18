;; #(over)tone
;; Leverages Overtone to actuate sound on triggers from wearable sensor events.
;;
(ns quipucamayoc.tone
  (:require [clojure.core.async :as a :refer [<! go-loop]]
            [quipucamayoc.comm :as comm]
            [overtone.core :refer :all]
            [overtone.synth.stringed :refer :all]
            [overtone.synth.sts :refer :all]
            [overtone.at-at :as at]))

(defonce sc-server (boot-internal-server))


;; ## Sample Sampled Instruments
;; ### Guitar Pluck

(defonce bs6 (load-sample "assets/guitar_pluck/bs6.wav"))
(definst play-bs6 [amp 1]
         (* (play-buf 1 bs6) amp))
(def static-bs6 (partial play-bs6 :amp 1))

(defonce bs8 (load-sample "assets/guitar_pluck/bs8.wav"))
(definst play-bs8 [amp 1]
         (* (play-buf 1 bs8) amp))
(def static-bs8 (partial play-bs8 :amp 1))

(defonce bs9 (load-sample "assets/guitar_pluck/bs9.wav"))
(definst play-bs9 [amp 1]
         (* (play-buf 1 bs9) amp))
(def static-bs9 (partial play-bs9 :amp 1))


;; ## Sound Helpers

(defn playat [time delta offset]
  (+ time (* offset delta)))

;; ## Axis based guitar trigger.

;; ### Guitars

(def lg-guitar (guitar))
(def md-guitar (guitar))
(def sm-guitar (guitar))

(defmulti axis-trigger
          "Triggers sub-methods when on a *large*, *medium*, or *small* change in device axis data happens."
          :action)

;; #### Large

(defmethod axis-trigger :large [msg]
    (case (:sensor msg)
      :x (guitar-pick lg-guitar 5 7)
      :y (guitar-pick lg-guitar 3 5)
      :z (guitar-pick lg-guitar 1 3)))

;; #### Medium

(defmethod axis-trigger :medium [msg]
    (case (:sensor msg)
      :x (guitar-pick md-guitar 5 7)
      :y (guitar-pick md-guitar 3 5)
      :z (guitar-pick md-guitar 1 3)))

;; #### Small

(defmethod axis-trigger :small [msg]
    (case (:sensor msg)
      :x (static-bs6)
      :y (static-bs8)
      :z (static-bs9)))

;; ## Event distributor

(defmulti control
          "Currently only triggers the `axis-trigger` but will filter all sound adjusting events based on their
          initial topic. As the number of trigger events grows this will keep the codebase more maintainable."
          :topic)

(defmethod control :axis-trigger [{:keys [msg]}]
  (axis-trigger msg)
  #_(println "Got" msg))

(defn init
  "Sets instruments to their initial state, starts listening for events."
  []
  (ctl lg-guitar :pre-amp 4 :distort 0.14 :noise-amp 0.82
       :lp-freq 3400 :lp-rq 4.5
       :rvb-mix 0.01 :rvb-room 0.01 :rvb-damp 0.5)

  (ctl md-guitar :pre-amp 4 :distort 0.12 :noise-amp 0.82
       :lp-freq 3400 :lp-rq 2.5
       :rvb-mix 0.01 :rvb-room 0.01 :rvb-damp 0.5)

  (ctl sm-guitar :pre-amp 4 :distort 0.10 :noise-amp 0.82
       :lp-freq 3400 :lp-rq 1.5
       :rvb-mix 0.01 :rvb-room 0.01 :rvb-damp 0.5)

  (go-loop []
    (when-let [v (<! comm/adjust-tone)]
      (control v)
      (recur))))