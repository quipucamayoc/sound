(ns quipucamayoc.visual
  (:require
    [clojure.core.async :as a :refer [<! go-loop]]
    [clojure.pprint :refer [pprint]]
    [quipucamayoc.comm :as comm]
    [quil.core :as q]
    [quil.middleware :as m]))

(comment

  (require :reload 'quipucamayoc.visual)

  )

(defmulti character :character)

;; Circle
(defmethod character 0 [{:keys [x y z trail colour weight]}]
  (let [s-weight (/ weight 2)]
    (q/push-matrix)

    (q/no-stroke)
    (q/fill (:r colour) (:g colour) (:b colour))
    (q/translate x y 576)

    (q/sphere s-weight)

    (q/pop-matrix)))

;; Rect
(defmethod character 1 [{:keys [x y z trail colour weight]}]
  (q/push-matrix)

  (q/no-stroke)
  (q/fill (:r colour) (:g colour) (:b colour))
  (q/translate (- x (/ 2 weight)) (- y (/ 2 weight)) 576)

  (q/box weight weight 3)

  (q/pop-matrix))

;; Triangle
(defmethod character 2 [{:keys [x y z trail colour weight]}]
  (q/push-matrix)

  (q/no-stroke)
  (q/fill (:r colour) (:g colour) (:b colour))
  (q/translate (- x (/ weight 2)) (- y (/ weight 2)) 576)

  (q/begin-shape)

  (q/vertex (/ weight 2) 0 (/ weight 2));
  (q/vertex 0 weight 0);
  (q/vertex weight weight 0);

  (q/end-shape)

  (q/pop-matrix))

;; Hexagon
(defmethod character 3 [{:keys [x y z trail colour weight]}]
  (q/push-matrix)

  (q/no-stroke)
  (q/fill (:r colour) (:g colour) (:b colour))
  (q/translate (- x (/ weight 2)) (- y (/ weight 2)) 576)

  (q/begin-shape)

  (q/vertex (/ weight 2) 0 (/ weight 2));
  (q/vertex 0 weight 0);
  (q/vertex weight weight 0);
  (q/vertex  weight 0 0);
  (q/vertex (/ weight 2) (/ weight 2) (/ weight 2));

  (q/end-shape)

  (q/pop-matrix))
;;
(defmethod character :default [{:keys [x y z trail colour weight]}]
  (let [s-weight (/ weight 2)]
    (q/push-matrix)

    (q/no-stroke)
    (q/fill (:r colour) (:g colour) (:b colour))
    (q/translate x y 576)

    (q/sphere s-weight)

    (q/pop-matrix)))

(defn setup []
  (q/frame-rate 60)
  (q/background 255)
  (q/smooth)
  (q/camera)
  (q/perspective)
  (q/lights)

  ; initial state
  {})

(defn check-in [state key op a b]
  (if (op (key state) a)
    (assoc state key b)
    state))

(defn update-step [state]
  ; increase radius of the circle by 1 on each frame
  #_(update-in state [:r] inc)
  (first @comm/device-history)
  #_(-> (first comm/device-history)
      (check-in :x > 1047 427)
      (check-in :x < 427 1047)
      (check-in :y > 623 283)
      (check-in :y < 283 623)
      (check-in :z > 576 236)
      (check-in :z < 236 576)))

(defn draw [state]
  (q/background 255 255 255)
  (q/smooth)
  (q/camera)

  (let [fov (/ Math/PI 2.0)
        camera-z (/ (/ (q/height) 2.0) (Math/tan (/ fov 2)))]
    (q/perspective fov
                   (/ (+ 0.0 (q/width)) (+ 0.0 (q/height)))
                   (/ camera-z 10.0)
                   (* camera-z 10.0)))

  ;(q/ambient-light 225 225 225);
  (q/ambient 20, 20, 20);
  ;(q/specular 0, 0, 0);
  ;(q/shininess 0)
  ;(q/emissive 20 20 20)
  ;
  ;(q/directional-light 155 155 130    0.5 -0.5 -1);
  ;(q/directional-light 155 130 155    0.5  2    1);
  ;(q/directional-light 130 155 155   -2    0   -2);
  ;(q/directional-light 90 50 50       0   -3   -1);
  ;(q/directional-light 50 50 90       0    3    1);
  ;
  ;(q/push-matrix)
  ;(q/translate (/ (q/width) 2) (/ (q/height) 2) 0)
  ;
  ;
  ;;(q/no-fill)
  ;;(q/stroke 55 55 55 10)
  ;;(q/stroke-weight 4)
  ;;(q/scale 1.25 0.8 1)
  ;;(q/box (q/width))
  ;
  ;
  ;(q/pop-matrix)

  (doseq [st (vals state)]
    (if (and   (not= false (:x st))
               (not= false (:y st))
               (not= false (:z st))
               (not= false (:color st))
               (not= false (:weight st))
               (not= false (:character st)))
      (character st))))


(defn mouse-moved [state event]
  state
  #_(let [px (vec (drop 1 (conj (:xp state) (:x event))))
        py (vec (drop 1 (conj (:yp state) (:y event))))]

    ;(println (:x event))
    ;(assoc state :x (:x event) :y (:y event))
    ; decrease radius
    #_(update-in [:r] shrink)
    ))

(defn key-typed [state event]
  state
  ;(println state)
  #_(let [key (:key event)]
    (cond
      (= :w key) (assoc state :y (- (:y state) 10))
      (= :a key) (assoc state :x (- (:x state) 10))
      (= :s key) (assoc state :y (+ (:y state) 10))
      (= :d key) (assoc state :x (+ (:y state) 10))
      (= :q key) (assoc state :z (+ (:z state) 10))
      (= :e key) (assoc state :z (- (:z state) 10))
      :else state)))

(defn mouse-clicked [state event]
  state)

(defn focus-lost [state]
  state)

(defn focus-gained [state]
  state)

