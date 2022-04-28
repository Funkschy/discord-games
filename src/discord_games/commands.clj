(ns discord-games.commands
  (:require
   [clojure.string :as str]
   [discljord.messaging :as m]
   [discord-games.config :refer [config]]
   [discord-games.game :refer [status-text update-state]]
   [discord-games.game-2048 :as g2048]
   [discord-games.render :as r]
   [discord-games.state :refer [create-game! create-reactable-message!
                                has-running-game? message-game quit-game!
                                running-game update-game-state!]]
   [discord-games.wordle :as wordle]
   [discord-games.tictactoe :as tictactoe]
   [clojure.set :as s])
  (:import
   [java.io File]))

(def command-prefix (get-in config [:commands :prefix]))
(def img-channel (get-in config [:discord-constants :img-channel]))

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

(defn- ^File game-image-file [username]
  (File/createTempFile (str username "_game") ".png"))

(def ^:private img-embed {"title" "Game"
                          "type"  "image"
                          "image" {"url" ""}})

(defn- get-img-embed [messaging game file]
  (r/render-to-file! game file)
  (let [msg (m/create-message! messaging img-channel :attachments [file])
        url (get-in @msg [:attachments 0 :url])]
    (assoc-in img-embed ["image" "url"] url)))

(defn- update-game! [game-tag messaging author channel-id input]
  (let [{:keys [game message-id file-path]} (running-game author game-tag)
        file    (File. ^String file-path)
        updated (update-state game input)
        embed   (get-img-embed messaging updated file)]
    (update-game-state! author game-tag assoc :game updated)
    (m/edit-message! messaging
                     channel-id
                     message-id
                     :content (status-text updated)
                     :embed embed)))

(defn- start-game! [game-tag messaging author channel-id init-game map-msg]
  (let [file  (game-image-file author)
        embed (get-img-embed messaging init-game file)]
    (create-game! author game-tag {:game init-game :file-path (.getPath file)})
    (->> @(m/create-message! messaging
                             channel-id
                             :content (status-text init-game)
                             :embed embed)
         (map-msg)
         (:id)
         (update-game-state! author game-tag assoc :message-id))))

(defn- start-or-update! [game-tag messaging author channel-id msg-content start-fn]
  (let [input (command-arg-text msg-content)]
    (if (has-running-game? author game-tag)
      (update-game! game-tag messaging author channel-id input)
      (start-fn messaging author channel-id input))))

(defn- init-reactions! [game-tag messaging channel-id author emotes {id :id :as message}]
  (doseq [emote emotes]
    (m/create-reaction! messaging channel-id id emote))
  (create-reactable-message! author id game-tag)
  message)

;; --- wordle ---

(defn- start-wordle-game! [messaging author channel-id guess]
  (let [init-game (update-state (wordle/new-game) guess)]
    (prn init-game)
    (start-game! :wordle messaging author channel-id init-game identity)))

(defmethod execute! "wordle" [{:keys [messaging]} {:keys [author channel-id]} msg-content]
  (start-or-update! :wordle messaging author channel-id msg-content start-wordle-game!))

;; --- 2048 ---

(def ^:private dir->emote-2048 (select-keys (get-in config [:discord-constants :arrow-emotes])
                                            ["left" "right" "up" "down"]))

(def ^:private emote->dir-2048 (s/map-invert dir->emote-2048))

(defn- start-2048-game! [messaging author channel-id _]
  (let [init-game (g2048/new-game)
        emotes    (keys emote->dir-2048)
        map-msg   (partial init-reactions! :2048 messaging channel-id author emotes)]
    (start-game! :2048 messaging author channel-id init-game map-msg)))

(defmethod execute! "2048" [{:keys [messaging]} {:keys [author channel-id]} msg-content]
  (start-or-update! :2048 messaging author channel-id msg-content start-2048-game!))

(defmethod react! :2048 [{:keys [messaging]} {:keys [author channel-id]} emote message-id]
  (when-let [dir (emote->dir-2048 emote)]
    (update-game! :2048 messaging author channel-id dir)
    (m/delete-user-reaction! messaging channel-id message-id emote author)))

;; --- tic tac toe ---

(def ^:private tic-tac-toe-emote-order
  ["upper-left" "up" "upper-right" "left" "center" "right" "lower-left" "down" "lower-right"])
(def ^:private dir->emote-tictactoe (get-in config [:discord-constants :arrow-emotes]))
(def ^:private emote->dir-tictactoe (s/map-invert dir->emote-tictactoe))

(defn- start-tic-tac-toe-game! [messaging author channel-id _]
  (let [init-game (tictactoe/new-game)
        emotes    (map dir->emote-tictactoe tic-tac-toe-emote-order)
        map-msg   (partial init-reactions! :tictactoe messaging channel-id author emotes)]
    (start-game! :tictactoe messaging author channel-id init-game map-msg)))

(defmethod execute! "tictactoe" [{:keys [messaging]} {:keys [author channel-id]} msg-content]
  (start-or-update! :tictactoe messaging author channel-id msg-content start-tic-tac-toe-game!))

(def ^:private dir->index
  (->> tic-tac-toe-emote-order
       (map-indexed #(vector %2 (str %1)))
       (into {})))

(defmethod react! :tictactoe [{:keys [messaging]} {:keys [author channel-id]} emote message-id]
  (when-let [index   (-> emote emote->dir-tictactoe dir->index)]
    (update-game! :tictactoe messaging author channel-id index)
    (m/delete-user-reaction! messaging channel-id message-id emote author)))

;; --- general ---

(defmethod execute! "quit" [{:keys [messaging]} {:keys [author channel-id]} msg-content]
  (let [games {"wordle" :wordle "2048" :2048 "tictactoe" :tictactoe}
        game  (command-arg-text msg-content)]
    (if (games game)
      (do (quit-game! author (games game))
          (m/create-message! messaging channel-id :content (str "Quitting " game)))
      (m/create-message! messaging channel-id :content (str "No game called " game)))))

(defmethod execute! :default [_ _ _])
(defmethod react! :default [_ _ _ _])
