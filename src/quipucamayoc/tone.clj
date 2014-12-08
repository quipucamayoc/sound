(ns quipucamayoc.tone
  (:require [clojure.core.async :as a :refer [<! go-loop]]
            [quipucamayoc.comm :as comm]
            [overtone.core :as ot :refer :all]
            [overtone.at-at :as at]))

(defonce my-schedule (at/mk-pool))

(defonce sc-server (boot-internal-server))

(defonce main-sequence (atom [:a 36 :b 27 :c 24 :d 36 :e 41 :f 27 :g 24 :h 36 :i 27 :j 36 :k 27 :l 24]))

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

(definst soda2 [a 36 b 27 c 24 d 36 e 41 f 27 g 24 h 36 i 27 j 36 k 27 l 24]
         (let [f (map #(midicps
                        (duty:kr % 0
                                 (dseq [a d g j 36 27 24]
                                       INF)))
                      [1 1/4 1/8])
               tones (map
                       #(blip (* % %2)
                              (mul-add:kr
                                (lf-noise1:kr 1/8)
                                3
                                4))
                       f
                       [1 4 8])]
           (rlpf (* 0.5 (g-verb (sum tones) 200 8)))))


(defn bass [msg]
  (apply (ctl soda2) msg))

(defn init []
  (println "Init Sound Server")
  (soda2)
  (at/every 300
            #(comm/adjust-tone)
            my-schedule)
  (go-loop []
           (when-let [v (<! comm/adjust-tone-with)]
             (bass (:msg v))
           (recur))))