(ns discord-games.state
  (:require
   [clojure.edn :as edn]
   [discord-games.db :as db]
   [discord-games.wordle :refer [map->Wordle]]
   [discord-games.game-2048 :refer [map->Game2048]]
   [discord-games.tictactoe :refer [map->TicTacToe]]))

(defn- running-game [game user-id channel-id must-be-owner?]
  (first
   (db/exec-query (str "SELECT g.id AS id, g.game_state AS game_state
                        FROM running_game g
                        JOIN participant part
                        JOIN player p
                        JOIN message m
                        ON  g.id = part.game_id
                        AND p.id = part.player_id
                        AND g.id = m.game_id
                        WHERE game_tag = ?
                        AND p.id = ?
                        AND m.channel_id = ?"
                       (when must-be-owner?
                         "AND part.owner = 1"))
                  game user-id channel-id)))

(defn- get-game-state [game-result]
  (->> game-result
       (:game_state)
       (edn/read-string {:readers {'discord_games.wordle.Wordle map->Wordle
                                   'discord_games.game_2048.Game2048 map->Game2048
                                   'discord_games.tictactoe.TicTacToe map->TicTacToe}})))

(defn running-game-state [game user-id channel-id]
  (get-game-state (running-game game user-id channel-id false)))

(defn game-in-message [channel-id message-id]
  (->> (db/exec-query "SELECT g.game_tag
                       FROM message m
                       JOIN running_game g
                       ON m.game_id = g.id
                       WHERE channel_id = ?
                       AND m.id = ?"
                      channel-id message-id)
       first
       :game_tag))

(defn owns-running-game? [game user-id channel-id]
  (seq (running-game game user-id channel-id true)))

(defn has-running-game? [game user-id channel-id]
  (seq (running-game game user-id channel-id false)))

(defn create-game! [game owner-id channel-id message-id init-state & extra-participants]
  (when (owns-running-game? game owner-id channel-id)
    (throw (IllegalStateException. (str owner-id " already has a running game"))))

  (db/dotransaction
   (let [[game] (db/exec-update "INSERT INTO running_game (game_tag, game_state) VALUES (?, ?)"
                                game (prn-str init-state))
         game-id (first (vals game))]
     (doseq [[p owner?] (cons [owner-id 1] (map vector extra-participants (repeat 0)))]
       (db/exec-update "INSERT OR IGNORE INTO player (id) VALUES (?)" p)
       (db/exec-update "INSERT INTO participant (game_id, player_id, owner) VALUES (?, ?, ?)"
                       game-id p owner?))
     (db/exec-update "INSERT INTO message (id, channel_id, game_id) VALUES (?, ?, ?)"
                     message-id channel-id game-id))))

(defn quit-game! [game user-id channel-id]
  (when-let [game-id (:id (running-game game user-id channel-id true))]
    (db/exec-update "DELETE FROM running_game WHERE id = ?" game-id)))

(defn update-game-state! [game user-id channel-id f & args]
  (when-let [game (running-game game user-id channel-id false)]
    (let [game-state (get-game-state game)
          game-id    (:id game)]
      (db/exec-update "UPDATE running_game
                       SET game_state = ?
                       WHERE id = ?"
                      (prn-str (apply f game-state args)) game-id))))
