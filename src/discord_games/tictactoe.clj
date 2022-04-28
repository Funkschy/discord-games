(ns discord-games.tictactoe
  (:require
   [discord-games.game :refer [error Game ok]]
   [discord-games.render :refer [TextTileGame draw-letter!]]))

(defn- check-line [line]
  (cond (every? #{:x} line) :x
        (every? #{:o} line) :o
        :else nil))

(defn- rows [{fields :fields}]
  (partition 3 fields))

(defn- cols [{fields :fields}]
  (apply map list (partition 3 fields)))

(defn- diags [game-state]
  (list (map-indexed #(nth %2 %1) (rows game-state))
        (map-indexed #(nth %2 %1) (reverse (rows game-state)))))

(defn- find-winner [game-state]
  (->> (concat (rows game-state)
               (cols game-state)
               (diags game-state))
       (map check-line)
       (remove nil?)
       (first)))

(def ^:private get-next-turn {:x :o, :o :x})

(defn- valid-move? [{:keys [fields]} index]
  (and (int? index)
       (<= 0 index 8)
       (= :e (fields index))))

(defn- over? [{status :status}]
  (= :over status))

(defn- check-over [{:keys [fields] :as game-state}]
  (if (or (find-winner game-state)
          (not-any? #{:e} fields))
    (assoc game-state :status :over)
    game-state))

(defn- make-move [{:keys [turn] :as game-state} index]
  (-> game-state
      (assoc-in [:fields index] turn)
      (assoc :turn (get-next-turn turn))
      (check-over)))

(def letter-size 128)

(defrecord TicTacToe [fields turn])

(defn new-game []
  (TicTacToe. (vec (repeat 9 :e)) :x))

(def ^:private indices (into {} (map #(vector (str %) %) (range 9))))

(extend-type TicTacToe
  Game
  (update-state [game-state index]
    (let [index (indices index)]
      (cond
        (over? game-state) (ok game-state)
        (valid-move? game-state index) (make-move game-state index)
        :else (error game-state (str "invalid move: " index)))))
  (status-text [{:keys [turn] :as game-state}]
    (case (:status game-state)
      :over  (if-let [w (find-winner game-state)]
               (str w " won the game")
               "Draw")
      :error (:message game-state)
      (str "It's " turn "'s turn")))

  TextTileGame
  (draw-game! [{fields :fields} g w h]
    (let [bg-color  "#3a3a3c"
          turn-info {:x ["X" "#3c5e8b"] :o ["O" "#468232"] :e [" " bg-color]}
          tile-size (/ w 3)]
      (assert (= (/ w 3) (/ h 3)) "invalid image size")
      (doseq [[i [sym color]] (map-indexed vector (map turn-info fields))]
        (draw-letter! g (* tile-size (mod i 3)) (* tile-size (quot i 3)) tile-size tile-size sym color bg-color))))
  (dims [_]
    [(* 3 letter-size) (* 3 letter-size) letter-size]))
