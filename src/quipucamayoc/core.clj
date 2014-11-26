(ns quipucamayoc.core
  (:require [quipucamayoc.tone :as tone]
            [quipucamayoc.comm :as comm]
            [clojure.core.async :as async :refer [<! <!! go take! alt!!]]))

;; Removed Quil for now.

(defn -main [& args]
  (tone/init))
