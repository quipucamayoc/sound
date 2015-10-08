;; #(over)tone
;; Leverages Overtone to actuate sound on triggers from wearable sensor events.
;;
(ns sound.tone
  (:require [clojure.core.async :as a :refer [<! go-loop]]
            [sound.comm :as comm]
            [overtone.core :refer :all]
            [overtone.synth.stringed :refer :all]
            [overtone.synth.sts :refer :all]
            [overtone.at-at :as at]))

(defonce sc-server (boot-internal-server))

(def instrument (atom {:current :fire-fly}))

;; ## Sample Sampled Instruments
;; ### Guitar Pluck

(defonce bs5 (load-sample "assets/ws_two/Birth of Pariacaca/Short/Siku 1.wav"))
(definst play-bs5 [amp 1]
         (* amp (play-buf 1 bs5)))
(def static-bs5 (partial play-bs5 :amp 0.5))

(defonce bs6 (load-sample "assets/ws_two/Birth of Pariacaca/Short/Siku 2.wav"))
(definst play-bs6 [amp 1]
         (* amp (play-buf 1 bs6)))
(def static-bs6 (partial play-bs6 :amp 0.5))

(defonce bs8 (load-sample "assets/ws_two/Birth of Pariacaca/Short/Siku 3.wav"))
(definst play-bs8 [amp 1]
         (* (play-buf 1 bs8) amp))
(def static-bs8 (partial play-bs8 :amp 0.5))

(defonce bs9 (load-sample "assets/ws_two/Birth of Pariacaca/Short/Siku 4.wav"))
(definst play-bs9 [amp 1]
         (* (play-buf 1 bs9) amp))
(def static-bs9 (partial play-bs9 :amp 0.5))


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
      :x (static-bs5)
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

;; ## Sample Blend

(defonce wind (load-sample "assets/ws_two/Birth of Pariacaca/Long Tracks/Joel and Ronald.wav"))
(defonce rain (load-sample "assets/ws_two/Birth of Pariacaca/Long Tracks/Polar Wind.wav"))
(defonce thunder (load-sample "assets/ws_two/Birth of Pariacaca/Long Tracks/Rain.wav"))

(defonce flies (load-sample "assets/ws_two/Masoma/Long Tracks/Flies.wav"))
(defonce fire-harsh (load-sample "assets/ws_two/Masoma/Long Tracks/Omar and Joel.wav"))
(defonce fire-soft (load-sample "assets/ws_two/Masoma/Long Tracks/Quechua text read out loud.wav"))

;; #### Snapping Wind and Flies

(definst fly-wind [vola 1 volb 0 volc 0]
         (let [a (* (* volb 1.7) (play-buf :num-channels 1 :bufnum thunder :loop 1))
               b (* (* volb 1.7) (play-buf :num-channels 1 :bufnum wind :loop 1))
               c (* (* volc 1.5) (play-buf :num-channels 1 :bufnum rain :loop 1))]
           (mix [a b c])))

;; #### Snapping Fire and Flies

(definst fly-fire [vola 1 volb 0 volc 0]
         (let [a (* (* volb 1.7) (play-buf :num-channels 1 :bufnum thunder :loop 1))
               b (* (/ volb 2.5) (play-buf :num-channels 1 :bufnum fire-harsh :loop 1))
               c (* (* volc 2.0) (play-buf :num-channels 1 :bufnum fire-soft :loop 1))]
           (mix [a b c])))

;; To-Do, cleaner cuts.

;; #### Storm. Thunder. Wind.

(definst storm [vola 1 volb 0 volc 0]
         (let [a (* (* volb 2.1) (play-buf :num-channels 1 :bufnum flies :loop 1))
               b (* (* volb 1.7) (play-buf :num-channels 1 :bufnum rain :loop 1))
               c (* (* volc 1.5) (play-buf :num-channels 1 :bufnum wind :loop 1))]
           (mix [a b c])))

(defmulti sample-blend
          "Control (ctl) specific sample-blend instruments. Allows for constantly running background sounds."
          :action)

(defmethod sample-blend :fly-fire [msg]
  (let [[_ vola _ volb _ volc] (:data msg)]
    (ctl fly-fire :vola (if (nil? vola) 0 vola) :volb (if (nil? volb) 0 volb) :volc (if (nil? volc) 0 volc))))

(defmethod sample-blend :thunder-storm [msg]
  (let [[_ vola _ volb _ volc] (:data msg)]
    (ctl storm :vola (if (nil? vola) 0 vola) :volb (if (nil? volb) 0 volb) :volc (if (nil? volc) 0 volc))))

(defmethod sample-blend :fly-wind [msg]
  (let [[_ vola _ volb _ volc] (:data msg)]
    (ctl fly-wind :vola (if (nil? vola) 0 vola) :volb (if (nil? volb) 0 volb) :volc (if (nil? volc) 0 volc))))

(defn ctl-current [msg]
  (let [[_ vola _ volb _ volc] (:data msg)]
    (if (= :thunder-storm (:current @instrument))
      (ctl storm :vola (if (nil? vola) 0 vola) :volb (if (nil? volb) 0 volb) :volc (if (nil? volc) 0 volc))
      (ctl fly-fire :vola (if (nil? vola) 0 vola) :volb (if (nil? volb) 0 volb) :volc (if (nil? volc) 0 volc)))))

;; ## Event distributor

(defmulti control
          "Distributes, to appropriate sound generators, various trigger events from q/comm"
          :topic)

(defmethod control :axis-trigger [{:keys [msg]}]
  (axis-trigger msg))

(defmethod control :sample-blend [{:keys [msg]}]
  (ctl-current msg))

(defmethod control :change-inst [{:keys [msg]}]
  (println msg)
  (if (= :thunder-storm (:current @instrument))
    (do (reset! instrument {:current :fire-fly})
        (ctl storm :vola 0 :volb 0 :volc 0))
    (do (reset! instrument {:current :thunder-storm})
        (ctl fly-fire :vola 0 :volb 0 :volc 0))))

(defn init
  "Sets instruments to their initial state, starts listening for events."
  []
  (ctl lg-guitar :pre-amp 5 :distort 0.14 :noise-amp 0.82
       :lp-freq 3400 :lp-rq 4.5
       :rvb-mix 0.01 :rvb-room 0.01 :rvb-damp 0.5)

  (ctl md-guitar :pre-amp 5 :distort 0.12 :noise-amp 0.82
       :lp-freq 3400 :lp-rq 2.5
       :rvb-mix 0.01 :rvb-room 0.01 :rvb-damp 0.5)

  (ctl sm-guitar :pre-amp 5 :distort 0.10 :noise-amp 0.82
       :lp-freq 3400 :lp-rq 1.5
       :rvb-mix 0.01 :rvb-room 0.01 :rvb-damp 0.5)

  (fly-fire)
  (storm)

  (ctl fly-fire :vola 0 :volb 0 :volc 0)
  (ctl storm :vola 0 :volb 0 :volc 0)

  (go-loop []
    (when-let [v (<! comm/adjust-tone)]
      (control v)
      (recur))))