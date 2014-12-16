(ns quipucamayoc.core
  (:require [quipucamayoc.tone :as tone]
            [quipucamayoc.comm :as comm]))

(defn -main [& args]
  (comm/init)
  (tone/init))