(ns discord-games.game-2048
  (:require
   [discord-games.render :refer [TextTileGame draw-text!]]))

(defn- pad-n [n pad-front? coll]
  (if pad-front?
    (reverse (take n (concat (reverse coll) (repeat 0))))
    (take n (concat coll (repeat 0)))))

(def ^:private pad-4 (partial pad-n 4))

(defn- merge-adjacent [coll]
  (->> coll
       (partition-by identity) ;; group adjacent identical numbers
       (mapcat (partial partition-all 2)) ;; only sum 2 numbers [1 1 1 1] should be [2 2], not [4]
       (map (partial apply +))))

(defn- swipe-line [pad-front? line]
  (->> line
       (filter (comp not zero?))
       (merge-adjacent)
       (pad-4 pad-front?)))

(defn- swipe-lines [pad-front? lines]
  (map (partial swipe-line pad-front?) lines))

(defn- spawn-random [game-state]
  (let [board         (:board game-state)
        empty-indices (remove nil? (map-indexed #(when (zero? %2) %1) board))
        rand-index    (and (not-empty empty-indices) (rand-nth empty-indices))
        value         (rand-nth [2 2 4])]
    (if rand-index
      (assoc-in game-state [:board rand-index] value)
      (assoc game-state :status :lost))))

(def ^:private swap-lines-cols
  (partial apply map vector))

(def ^:private directions
  {:left [false identity]
   :right [true identity]
   :up   [false swap-lines-cols]
   :down [true swap-lines-cols]})

(defn- shift [game-state direction]
  (let [[pad-front? mapper] (directions direction)
        board (:board game-state)
        lines (partition 4 board)]
    (->> lines
         (mapper)
         (swipe-lines pad-front?)
         (mapper)
         (reduce concat)
         (vec)
         (assoc game-state :board))))

(defn- error [game-state message]
  (-> game-state
      (assoc :status :error)
      (assoc :message message)))

;; --- API ---

(def tile-size 128)

(defrecord Game2048 [board]
  TextTileGame
  (draw-game! [_ g w h]
    (let [text-color "#d7dadc"
          tile-size  (/ w 4)
          bg-color   "#3a3a3c"]
      (assert (= w h) "invalid image size")
      (loop [[line & tail] (partition 4 board), y 0]
        (doseq [[offset value] (map vector (range) line)]
          (draw-text! g
                      (* offset tile-size)
                      y
                      tile-size
                      tile-size
                      (str value)
                      text-color
                      bg-color))
        (when tail
          (recur tail (long (+ y tile-size)))))))

  (dims [_]
    [(* 4 tile-size) (* 4 tile-size) (/ tile-size 2)]))

(defn lost? [game-state]
  (or (:lost game-state)
      (->> [:left :up :right :down]
           (map (partial shift game-state))
           (apply =))))

(defn valid-move? [game-state direction]
  (not= (shift game-state direction) game-state))

(defn- make-move [game-state direction]
  (let [next-state (spawn-random (shift game-state direction))]
    (if (lost? next-state)
      (assoc next-state :status :lost)
      next-state)))

(def ^:private direction-strings
  {"left" :left, "l" :left
   "right" :right, "r" :right
   "up" :up, "u" :up
   "down" :down, "d" :down})

(defn move [game-state direction-str]
  (let [direction (direction-strings direction-str)]
    (cond
      (nil? direction) (error game-state (str "Invalid direction: " direction-str))
      (not (valid-move? game-state direction)) (error game-state "Invalid move")
      :else (make-move game-state direction))))

(defn new-game []
  (spawn-random (Game2048. (vec (repeat 16 0)))))

(defn status-text [game-state]
  (case (:status game-state)
    :error (:message game-state)
    :lost  (:message game-state)
    "2048"))
