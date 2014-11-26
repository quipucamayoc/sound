(ns quipucamayoc.core
  (:require [quil.core :as q]
            [quil.middleware :as m]
            [quipucamayoc.tone :as tone]
            [quipucamayoc.comm :as comm]
            [clojure.core.async :as async :refer [<! <!! go take! alt!!]]))

(def ioc-state (atom { 72834683264 { :id   1
                                     :uuid 72834683264
                                     :x    0
                                     :y    0
                                     :z    0
                                     :colo 200
                                     :conn true
                                     :sens []}}))


(defn show-frame-rate [options]
  (let [draw (:draw options (fn [state]))
        updated-draw (fn [state]
                       (draw state)
                       (q/fill 0)
                       (q/text-num (q/current-frame-rate) 10 20))]
    (assoc options :draw updated-draw)))


(defn setup []
  (q/smooth)
  (q/frame-rate 30)
  (q/color-mode :rgb)

  (let [hints [ :enable-depth-sort
                :disable-opengl-errors
                :enable-optimized-stroke
                :enable-retina-pixels
                :enable-stroke-perspective
                :enable-stroke-pure]]
    (doseq [h hints]
      (q/hint h)))

  ;; State
  {:x 0 :y 0})

(defn upkeep [state]
  (let []
    (comm/osc-heartbeat)
    ;(assoc state :beans @ioc-state)
    state))

(defn bean-rect [stats]
  (let [{:keys [x y z colo id uuid]} stats
        tx (if (> id 3)
             (* (- id 3) (/ (q/screen-width) 9))
             (* id (/ (q/screen-width) 9)))
        ty (if (> id 3)
             (/ (q/screen-height) 2)
             (/ (q/screen-height) 6))]
    (q/push-matrix)
    ;; Start Bean

    (q/translate tx ty 0)                                   ;; Moves into position

    (q/push-matrix)                                         ;; Isolates for rotation
    ; Bean Rect
    (q/fill colo)
    (q/rotate-x x)
    (q/rotate-y y)
    (q/rotate-z z)
    (q/box 80 20 160)
    ; E Bean Rect
    (q/pop-matrix)

    (q/translate 50 50 20)                                  ;; Align Bean UI
    ; Text UUID
    (q/fill 255)
    (q/text (str uuid) -80 28 0)
    (q/fill 0)
    (q/text (str uuid) -81 27 1)
    ; Connection Indicator
    (q/fill colo)
    (q/sphere-detail 90)
    (q/sphere 10)

    ;End Bean
    (q/pop-matrix)))

(defn draw [state]
  (q/lights)
  (q/background 240)
  (q/no-stroke)
  (let [beans @ioc-state]
    (zipmap (keys beans) (map bean-rect (vals beans)))))


(comment defn mouse-moved [state event]
  (-> state
      (assoc :x (:x event) :y (:y event))))


(q/defsketch bean-core
         :title "Quipucamayoc Core"
         :size [1024 768]
         :setup setup
         :draw draw
         :update upkeep
         ;:mouse-moved mouse-moved
         :renderer :opengl
         :middleware [m/fun-mode show-frame-rate])

(defn reciever []
  (println "Start Reciever")
  (go (loop []
        (when-let [v (<! comm/adjust-quil-with)]
          (println (:msg v))
          (reset! ioc-state (:msg v))
          (recur)))))

(defn -main [& args]
  (reciever)
  (tone/init))
