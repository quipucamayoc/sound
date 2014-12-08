(ns quipucamayoc.tone
  (:require [clojure.core.async :as a :refer [<! go-loop]]
            [quipucamayoc.comm :as comm]
            [overtone.core :as ot :refer :all]
            [overtone.at-at :as at]))

(defonce my-schedule (at/mk-pool))

(defonce sc-server (boot-internal-server))

(definst soda [
              a 24 ;; Note 1
              b 27 ;; Note 2
              c 31 ;; Note 3
              d 35 ;; Note 3
              fa 1 ;; 1 - 6
              fb 4 ;; 1 - 6
              fc 8 ;; 1 - 6
              fd 1 ;; 1 - 6
              ]
         (let [f (map #(midicps
                        (duty:kr % 0
                                 (dseq [a b c d]
                                       INF)))
                      [(/ 1 fa) (/ 1 fb) (/ 1 fc) (/ 1 fd)])
               tones (map
                       #(blip (* % %2)
                              (lf-noise0:kr (/ 1 (+ 1(/ (+ fa fb) 2)))))
                       f
                       [fd fc fb fa])]
           (rlpf (* 0.5 (g-verb (sum tones) 200 8 (/ 1 fa) (/ 1 fb) (/ 1 fc))))))

(defn control [{:keys [a b c d fa fb fc fd]}]
  (ctl soda :a a :b b :c c :d d :fa fa :fb fb :fc fc :fd fd))

(defn init []
  (println "Init Sound Server")
  (soda)
  (at/every 300
            #(comm/adjust-tone)
            my-schedule)
  (go-loop []
           (when-let [v (<! comm/adjust-tone-with)]
             (control (:msg v))
           (recur))))