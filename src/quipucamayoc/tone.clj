(ns quipucamayoc.tone
  (:require [clojure.core.async :as a :refer [<! go-loop]]
            [quipucamayoc.comm :as comm]
            [quil.core :refer [map-range]]
            [overtone.core :refer :all]
            [overtone.at-at :as at]))

(defonce my-schedule (at/mk-pool))

(defonce sc-server (boot-internal-server))

(defn by-weight [w note]
  (case w
    1 (map-range note 22 43 26 40)
    2 (map-range note 22 43 29 43)
    3 (map-range note 22 43 22 36)
    4 (map-range note 22 43 22 40)
    note))

(definst guitar [frequency 240 duration 10
                 h0 1 h1 0.5 h2 0.3 h3 0.25 h4 0.2 h5 0.16 h6 0.14
                 a 24                                         ;; Note 1
                 b 27                                         ;; Note 2
                 c 31                                         ;; Note 3
                 fa 1                                         ;; 1 - 6
                 fb 4                                         ;; 1 - 6
                 fc 8                                         ;; 1 - 6
                 amp 0.9                                      ;; 0.1 - 0.9
                 ]
         (let [harmonic-series [ 1 2 3 4 5 6 7]
               proportions [h0 h1 h2 h3 h4 h5 h6]
               component (fn [harmonic proportion]
                           (* 1/2
                              proportion
                              ;(env-gen (perc 0.01 (* proportion duration)))
                              (sin-osc (* harmonic (midicps (duty:kr (/ 1 fa) 0
                                                                     (dseq [a b c]
                                                                           INF)))))
                              ))
               whole (mix (map component harmonic-series proportions))]
           (detect-silence whole :action FREE)
           (* (+ 0.45 amp) whole)))

(definst soda [
               a 24                                         ;; Note 1
               b 27                                         ;; Note 2
               c 31                                         ;; Note 3
               fa 1                                         ;; 1 - 6
               fb 4                                         ;; 1 - 6
               fc 8                                         ;; 1 - 6
               amp 0.9                                      ;; 0.1 - 0.9
               ]
         (let [f (map #(midicps
                        (duty:kr % 0
                                 (dseq [a b c]
                                       INF)))
                      [(/ 1 fa) (/ 1 fb) (/ 1 fc)])
               tones (map
                       #(blip (* % %2)
                              (lf-noise0:kr (/ 1 fc)))
                       f
                       [fc fb fa])]
           (* amp (rlpf (* 0.5 (g-verb (/ (sum tones) 3) 200 8))))))

(def instruments (atom []))

(defn control [msg]
  (dorun (map (fn [i w {:keys [a b c fa fb fc]}]
                (ctl i
                     :amp  (/ w 10)
                     :a (by-weight w a)
                     :b (by-weight w b)
                     :c (by-weight w c)
                     :fa fa
                     :fb fb
                     :fc fc))
              @instruments
              [1 2 3 4]
              msg)))

(defn init []
  (println "Init Sound Server")

  (let [soda1 (guitar :amp 0.01)
        soda2 (soda :amp 0.01)
        soda3 (soda :amp 0.01)
        soda4 (guitar :amp 0.01)]
    (reset! instruments [soda1 soda2 soda3 soda4]))

  (at/every 150
            #(comm/adjust-tone)
            my-schedule)
  (go-loop []
    (when-let [v (<! comm/adjust-tone-with)]
      (control (:msg v))
      (recur))))