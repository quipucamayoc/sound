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

;; ## Sample Instruments
;; ### Guitar Pluck
(defonce bs1 (load-sample "assets/guitar_pluck/bs1.wav"))
(definst play-bs1 [amp 1]
         (* (play-buf 1 bs1) amp))
(def static-bs1 (partial play-bs1 :amp 1))

(defonce bs2 (load-sample "assets/guitar_pluck/bs2.wav"))
(definst play-bs2 [amp 1]
         (* (play-buf 1 bs2) amp))
(def static-bs2 (partial play-bs2 :amp 1))

(defonce bs3 (load-sample "assets/guitar_pluck/bs3.wav"))
(definst play-bs3 [amp 1]
         (* (play-buf 1 bs3) amp))
(def static-bs3 (partial play-bs3 :amp 1))

(defonce bs4 (load-sample "assets/guitar_pluck/bs4.wav"))
(definst play-bs4 [amp 1]
         (* (play-buf 1 bs4) amp))
(def static-bs4 (partial play-bs4 :amp 1))

(defonce bs5 (load-sample "assets/guitar_pluck/bs5.wav"))
(definst play-bs5 [amp 1]
         (* (play-buf 1 bs5) amp))
(def static-bs5 (partial play-bs5 :amp 1))

(defonce bs6 (load-sample "assets/guitar_pluck/bs6.wav"))
(definst play-bs6 [amp 1]
         (* (play-buf 1 bs6) amp))
(def static-bs6 (partial play-bs6 :amp 1))

(defonce bs7 (load-sample "assets/guitar_pluck/bs7.wav"))
(definst play-bs7 [amp 1]
         (* (play-buf 1 bs7) amp))
(def static-bs7 (partial play-bs7 :amp 1))

(defonce bs8 (load-sample "assets/guitar_pluck/bs8.wav"))
(definst play-bs8 [amp 1]
         (* (play-buf 1 bs8) amp))
(def static-bs8 (partial play-bs8 :amp 1))

(defonce bs9 (load-sample "assets/guitar_pluck/bs9.wav"))
(definst play-bs9 [amp 1]
         (* (play-buf 1 bs9) amp))
(def static-bs9 (partial play-bs9 :amp 1))

;; ### Toy Piano

(defonce tp1 (load-sample "assets/toy_piano/125623__noisecollector__pianohit1.wav"))
(definst play-tp1 [amp 0.5]
         (* (play-buf 1 tp1) amp))
(def static-tp1 (partial play-tp1 :amp 0.5))

(defonce tp2 (load-sample "assets/toy_piano/125624__noisecollector__pianohit2.wav"))
(definst play-tp2 [amp 0.5]
         (* (play-buf 1 tp2) amp))
(def static-tp2 (partial play-tp2 :amp 0.5))

(defonce tp3 (load-sample "assets/toy_piano/125625__noisecollector__pianohit3.wav"))
(definst play-tp3 [amp 0.5]
         (* (play-buf 1 tp3) amp))
(def static-tp3 (partial play-tp3 :amp 0.5))

(defonce tp4 (load-sample "assets/toy_piano/125627__noisecollector__pianonewhit2.wav"))
(definst play-tp4 [amp 0.5]
         (* (play-buf 1 tp4) amp))
(def static-tp4 (partial play-tp4 :amp 0.5))

(defonce tp5 (load-sample "assets/toy_piano/125628__noisecollector__pianonewhit3.wav"))
(definst play-tp5 [amp 0.5]
         (* (play-buf 1 tp5) amp))
(def static-tp5 (partial play-tp5 :amp 0.5))

(defonce tp6 (load-sample "assets/toy_piano/125629__noisecollector__pianonewhit4.wav"))
(definst play-tp6 [amp 0.5]
         (* (play-buf 1 tp6) amp))
(def static-tp6 (partial play-tp6 :amp 0.5))

(defonce tp7 (load-sample "assets/toy_piano/125626__noisecollector__pianonewhit1.wav"))
(definst play-tp7 [amp 0.5]
         (* (play-buf 1 tp7) amp))
(def static-tp7 (partial play-tp7 :amp 0.5))



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
  (println "Got" msg))

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