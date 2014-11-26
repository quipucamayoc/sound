(ns quipucamayoc.tone
  (:require [clojure.core.async :as a :refer [<! go-loop]]
            [quipucamayoc.comm :as comm]
            [overtone.core :as ot :refer :all]))

(defonce sc-server (boot-internal-server))

;(def m-90 (metronome 120))
;(def scrape [(freesound 10605)
;             (freesound 10606)
;             (freesound 10607)
;             (freesound 10604)
;             (freesound 10603)
;             (freesound 10602)
;             (freesound 10601)])
;
;(def pull [(freesound 10566)
;             (freesound 10565)
;             (freesound 10564)
;             (freesound 10567)
;             (freesound 10568)
;             (freesound 10570)
;             (freesound 10569)])

;(definst scraper [item 0] (play-buf 1 item))

(definst soda [a 24 b 27 c 31 fa 1 fb 1/4 fc 1/8]
         (let [f (map #(midicps
                        (duty:kr % 0
                                 (dseq [a b c 36 41 27 24]
                                       INF)))
                      [fa fb fc])
               tones (map
                       #(blip (* % %2)
                              (mul-add:kr
                                (lf-noise1:kr 1/8)
                                3
                                4))
                       f
                       [1 4 8])]
           (rlpf (* 0.5 (g-verb (sum tones) 200 8)))))

(defn bass [{:keys [nota notb notc fa fb fc]}]
  (ctl soda :a nota :b notb :c notc :fa fa :fb fb :fc fc))

(defn init []
  (println "Init Sound Server")
  (soda)
  (go-loop []
           (when-let [v (<! comm/adjust-tone-with)]
             (bass (:msg v))
           (recur))))