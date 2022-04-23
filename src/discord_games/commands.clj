(ns discord-games.commands
  (:require
   [clojure.string :as str]
   [discljord.messaging :as m]
   [discord-games.config :refer [config]]
   [discord-games.game-2048 :as g2048]
   [discord-games.render :as r]
   [discord-games.state :refer [create-game! create-reactable-message!
                                has-running-game? message-game quit-game!
                                running-game update-game-state!]]
   [discord-games.wordle :as wordle])
  (:import
   [discord_games.game_2048 Game2048]
   [discord_games.wordle Wordle]
   [java.io File]))

(def command-prefix (get-in config [:commands :prefix]))
(def img-channel (get-in config [:discord-constants :img-channel]))

(defprotocol Game
  (update-state [game input])
  (status-text  [game]))

(extend-protocol Game
  Wordle
  (update-state [game guess]
    (wordle/make-guess game guess))
  (status-text [game]
    (wordle/status-text game))
  Game2048
  (update-state [game direction]
    (g2048/move game direction))
  (status-text [game]
    (g2048/status-text game)))

(defn command-name [msg-content]
  (when (str/starts-with? msg-content command-prefix)
    (->> (str/split msg-content #"\s+")
         (first)
         (rest)
         (apply str))))

(defn command-arg-text [msg-content]
  (->> msg-content
       (drop-while (comp not #{\space \tab}))
       (apply str)
       str/trim))

(defmulti execute!
  (fn [state context msg-content]
    (command-name msg-content)))

(defmulti react!
  (fn [state context emote message-id]
    (message-game (:author context) message-id)))

(defn- game-image-file [username]
  (File/createTempFile (str username "_game") ".png"))

(defn- get-img-embed [messaging game file]
  (r/render-to-file! game file)
  (let [msg (m/create-message! messaging img-channel :attachments [file])
        url (get-in @msg [:attachments 0 :url])]
    {"title" "Game"
     "type"  "image"
     "image" {"url" url}}))

(defn- update-game! [game-tag messaging author channel-id input]
  (let [{:keys [game file message]} (running-game author game-tag)
        updated (update-state game input)
        embed   (get-img-embed messaging updated file)]
    (update-game-state! author game-tag assoc :game updated)
    (m/edit-message! messaging
                     channel-id
                     message
                     :content (status-text updated)
                     :embed embed)))

(defn- start-game! [game-tag messaging author channel-id init-game map-msg]
  (let [file  (game-image-file author)
        embed (get-img-embed messaging init-game file)]
    (create-game! author game-tag {:game init-game :file file})
    (->> @(m/create-message! messaging
                             channel-id
                             :content (status-text init-game)
                             :embed embed)
         (map-msg)
         (:id)
         (update-game-state! author game-tag assoc :message))))

(defn- start-or-update! [game-tag messaging author channel-id msg-content start-fn]
  (let [input (command-arg-text msg-content)]
    (if (has-running-game? author game-tag)
      (update-game! game-tag messaging author channel-id input)
      (start-fn messaging author channel-id input))))

;; --- wordle ---

(defn- start-wordle-game! [messaging author channel-id guess]
  (let [init-game (wordle/make-guess (wordle/new-game) guess)]
    (start-game! :wordle messaging author channel-id init-game identity)))

(defmethod execute! "wordle" [{:keys [messaging]} {:keys [author channel-id]} msg-content]
  (start-or-update! :wordle messaging author channel-id msg-content start-wordle-game!))

;; --- 2048 ---

(defn- init-2048-reactions! [messaging channel-id author {id :id :as message}]
  (doseq [emote (keys (get-in config [:discord-constants :arrow-emotes]))]
    (m/create-reaction! messaging channel-id id emote))
  (create-reactable-message! author id :2048)
  message)

(defn- start-2048-game! [messaging author channel-id _]
  (let [init-game (g2048/new-game)
        map-msg   (partial init-2048-reactions! messaging channel-id author)]
    (start-game! :2048 messaging author channel-id init-game map-msg)))

(defmethod execute! "2048" [{:keys [messaging]} {:keys [author channel-id]} msg-content]
  (start-or-update! :2048 messaging author channel-id msg-content start-2048-game!))

(defmethod react! :2048 [{:keys [messaging]} {:keys [author channel-id]} emote message-id]
  (when-let [dir (get-in config [:discord-constants :arrow-emotes emote])]
    (update-game! :2048 messaging author channel-id dir)
    (m/delete-user-reaction! messaging channel-id message-id emote author)))

;; --- general ---

(defmethod execute! "quit" [{:keys [messaging]} {:keys [author channel-id]} msg-content]
  (let [games {"wordle" :wordle "2048" :2048}
        game  (command-arg-text msg-content)]
    (if (games game)
      (do (quit-game! author (games game))
          (m/create-message! messaging channel-id :content (str "Quitting " game)))
      (m/create-message! messaging channel-id :content (str "No game called " game)))))

(defmethod execute! :default [_ _ _])
(defmethod react! :default [_ _ _ _])
