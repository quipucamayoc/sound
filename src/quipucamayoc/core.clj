;; # Core = Start ☃
;; The main enrty point. Contained screen-based visualization via Quil. Will be added back once sound is finalized.
;;
(ns quipucamayoc.core
  (:require [quipucamayoc.tone :as tone]
            [quipucamayoc.comm :as comm]))

(defn -main [& args]
  (comm/init)
  (tone/init))