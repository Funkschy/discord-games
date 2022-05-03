(ns discord-games.state
  (:require
   [clojure.edn :as edn]
   [discord-games.db :as db]
   [discord-games.wordle :refer [map->Wordle]]
   [discord-games.game-2048 :refer [map->Game2048]]
   [discord-games.tictactoe :refer [map->TicTacToe]]))

(defn- running-game [game user-id channel-id]
  (first
   (db/exec-query "SELECT game_state
                   FROM running_game
                   WHERE game_tag = ?
                   AND player_one = ?
                   AND channel_id = ?"
                  game user-id channel-id)))

(defn running-game-state [game user-id channel-id]
  (->> (running-game game user-id channel-id)
       (:game_state)
       (edn/read-string {:readers {'discord_games.wordle.Wordle map->Wordle
                                   'discord_games.game_2048.Game2048 map->Game2048
                                   'discord_games.tictactoe.TicTacToe map->TicTacToe}})))

(defn message-game [user-id channel-id message-id]
  ;; FIXME co_players
  (->> (db/exec-query "SELECT game_tag
                       FROM running_game
                       WHERE player_one = ?
                       AND channel_id = ?
                       AND message_id = ?"
                      user-id channel-id message-id)
       first
       :game_tag))

(defn has-running-game? [game user-id channel-id]
  (seq (running-game game user-id channel-id)))

(defn create-game! [game user-id channel-id init-state]
  (db/exec-update "INSERT OR IGNORE INTO player (id) VALUES (?)" user-id)
  (when (has-running-game? game user-id channel-id)
    (throw (IllegalStateException. (str user-id " already has a running game"))))
  (db/exec-update "INSERT INTO running_game (game_tag, channel_id, player_one, game_state) VALUES (?, ?, ?, ?)"
                  game channel-id user-id (prn-str init-state)))

(defn add-participants! [game owner-id channel-id participants]
  (when-let [game-data (running-game game owner-id channel-id)]
    (doseq [p participants]
      (db/exec-update "INSERT OR IGNORE INTO player (id) VALUES (?)" p)
      (db/exec-update "INSERT OR IGNORE INTO co_player (game_tag, channel_id, player_one, player) VALUES (?, ?, ?, ?)"
                      game channel-id owner-id p))))

(defn create-reactable-message! [game user-id channel-id message-id]
  (db/exec-update "UPDATE running_game
                   SET message_id = ?
                   WHERE game_tag = ?
                   AND player_one = ?
                   AND channel_id = ?"
                  message-id game user-id channel-id))

(defn quit-game! [game user-id channel-id]
  (db/exec-update "DELETE FROM running_game
                   WHERE game_tag = ?
                   AND player_one = ?
                   AND channel_id = ?"
                  game user-id channel-id))

(defn update-game-state! [game user-id channel-id f & args]
  (when-let [game-state (running-game-state game user-id channel-id)]
    (db/exec-update "UPDATE running_game
                     SET game_state = ?
                     WHERE game_tag = ?
                     AND channel_id = ?
                     AND player_one = ?"
                    (prn-str (apply f game-state args)) game channel-id user-id)))
