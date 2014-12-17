(ns quipucamayoc.tone
  (:require [clojure.core.async :as a :refer [<! go-loop]]
            [quipucamayoc.comm :as comm]
            [quil.core :refer [map-range]]
            [overtone.core :refer :all]
            [overtone.synth.stringed :refer [guitar-pick guitar-strum]]
            [overtone.at-at :as at]))

(defonce sc-server (boot-internal-server))

(defmulti control :topic)

(defmethod control :axis-trigger [{:keys [msg]}]
  (println "Got" msg)
  (case (:action msg)
    :large  (guitar-strum)
    :medium (guitar-strum)
    :small  (guitar-strum)))

(defn init []
  (println "Init Sound Server")

  (go-loop []
    (when-let [v (<! comm/adjust-tone)]
      (control v)
      (recur))))