;; # Core = Start ☃
;; The main enrty point. Contained screen-based visualization via Quil. Will be added back once sound is finalized.
;;
(ns sound.core
  (:require
    [sound.tone :as tone]
    [sound.comm :as comm]
    ;[sound.db :as db]
    ;[sound.visual :as visual]
    ;[quil.core :as q]
    ;[quil.middleware :as m]
    ))


(defn -main [& args]

  (comm/init)

  ;(q/sketch
  ;             :title "Q/Core"
  ;             :features [:present :exit-on-close]
  ;             :bgcolor "#ffffff"
  ;             :display 0
  ;             :renderer :opengl
  ;             :size :fullscreen
  ;             :setup visual/setup
  ;             :draw visual/draw
  ;             :update visual/update-step
  ;             :mouse-moved visual/mouse-moved
  ;             ;:focus-lost visual/focus-lost
  ;             ;:focus-gained visual/focus-gained
  ;             :key-typed visual/key-typed
  ;             :mouse-clicked visual/mouse-clicked
  ;             :middleware [m/fun-mode])

  #_(db/init)


  (tone/init))