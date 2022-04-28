(ns discord-games.game)

(defprotocol Game
  (update-state [game input] "perform a move and return the updated gamestate")
  (status-text [game] "get the status of the game in a human readable format"))

(defn error [game-state message]
  (-> game-state
      (assoc :status :error)
      (assoc :message message)))

(defn ok [game-state]
  (if (= (:status game-state) :error)
    (dissoc game-state :status :message)
    game-state))
