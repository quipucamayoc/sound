(ns quipucamayoc.db
  (:require [monger.core :as mg]
            [monger.gridfs :as gfs :refer [store-file make-input-file filename content-type metadata]]))


(defn init []
  (let [conn (mg/connect)
        db   (mg/get-db conn "quipu-audio")
        fs   (mg/get-gridfs conn "quipu-audio")]

    #_(store-file (make-input-file fs "/Users/boriskourt/Development/quipucamayoc/nigh-quil/assets/guitar_pluck/bs1.wav")
          (filename "bs1.wav")
          (metadata {:format "wav"
                     :tags [:guitar]})
          (content-type "audio/wav"))

    (clojure.pprint/pprint (gfs/find fs {:filename "bs1.wav"}))))
