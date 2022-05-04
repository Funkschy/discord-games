(ns discord-games.commands
  (:require
   [clojure.set :as s]
   [clojure.string :as str]
   [discljord.messaging :as m]
   [discord-games.config :refer [config]]
   [discord-games.game :refer [status-text update-state]]
   [discord-games.game-2048 :as g2048]
   [discord-games.render :as r]
   [discord-games.state :refer [create-game!
                                game-in-message has-running-game? quit-game!
                                running-game-state update-game-state!]]
   [discord-games.tictactoe :as tictactoe]
   [discord-games.wordle :as wordle])
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
  (fn [state {:keys [channel-id]} emote message-id]
    (game-in-message channel-id message-id)))

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

(defn- update-game! [game-tag {:keys [messaging channel-id author]} input]
  (when-let [state (running-game-state game-tag author channel-id)]
    (let [{:keys [game file-path message-id]} state
          file    (File. ^String file-path)
          updated (update-state game input)
          embed   (get-img-embed messaging updated file)]
      (update-game-state! game-tag author channel-id assoc :game updated)
      (m/edit-message! messaging
                       channel-id
                       message-id
                       :content (status-text updated)
                       :embed embed))))

(defn- start-game! [game-tag {:keys [channel-id messaging author]} init-game map-msg & challengees]
  (let [file   (game-image-file author)
        embed  (get-img-embed messaging init-game file)
        msg    (m/create-message! messaging
                                  channel-id
                                  :content (status-text init-game)
                                  :embed embed)
        msg-id (->> @msg (map-msg) :id)
        state  {:game init-game :file-path (.getPath file) :message-id msg-id}]
    (apply create-game! game-tag author channel-id msg-id state challengees)))

(defn- start-or-update! [game-tag {:keys [author channel-id] :as env} msg-content start-fn]
  (let [input (command-arg-text msg-content)]
    (if (has-running-game? game-tag author channel-id)
      (update-game! game-tag env input)
      (start-fn env input))))

(defn- init-reactions! [{:keys [messaging channel-id]} emotes {id :id :as message}]
  (doseq [emote emotes]
    (m/create-reaction! messaging channel-id id emote))
  message)

;; --- wordle ---

(defn- start-wordle-game! [env guess]
  (let [init-game (update-state (wordle/new-game) guess)]
    (start-game! "wordle" env init-game identity)))

(defmethod execute! "wordle" [state context msg-content]
  (start-or-update! "wordle" (merge state context) msg-content start-wordle-game!))

;; --- 2048 ---

(def ^:private dir->emote-2048 (select-keys (get-in config [:discord-constants :arrow-emotes])
                                            ["left" "right" "up" "down"]))

(def ^:private emote->dir-2048 (s/map-invert dir->emote-2048))

(defn- start-2048-game! [env _]
  (let [init-game (g2048/new-game)
        emotes    (keys emote->dir-2048)
        map-msg   (partial init-reactions! env emotes)]
    (start-game! "2048" env init-game map-msg)))

(defmethod execute! "2048" [state context msg-content]
  (start-or-update! "2048" (merge state context) msg-content start-2048-game!))

(defmethod react! "2048" [{:keys [messaging] :as state} {:keys [channel-id author] :as context} emote message-id]
  (when-let [dir (emote->dir-2048 emote)]
    (update-game! "2048" (merge state context) dir)
    (m/delete-user-reaction! messaging channel-id message-id emote author)))

;; --- tic tac toe ---

(def ^:private tic-tac-toe-emote-order
  ["upper-left" "up" "upper-right" "left" "center" "right" "lower-left" "down" "lower-right"])
(def ^:private dir->emote-tictactoe (get-in config [:discord-constants :arrow-emotes]))
(def ^:private emote->dir-tictactoe (s/map-invert dir->emote-tictactoe))

(defn- start-tic-tac-toe-game! [{:keys [messaging guild-id] :as env} challenged-username]
  (when-let [challenged-user (first @(m/search-guild-members! messaging guild-id challenged-username))]
    (let [init-game  (tictactoe/new-game)
          emotes     (map dir->emote-tictactoe tic-tac-toe-emote-order)
          map-msg    (partial init-reactions! env emotes)
          challengee (get-in challenged-user [:user :id])]
      (start-game! "tictactoe" env init-game map-msg challengee))))

(defmethod execute! "tictactoe" [state context msg-content]
  (start-or-update! "tictactoe" (merge state context) msg-content start-tic-tac-toe-game!))

(def ^:private dir->index
  (->> tic-tac-toe-emote-order
       (map-indexed #(vector %2 (str %1)))
       (into {})))

(defmethod react! "tictactoe" [{:keys [messaging] :as state} {:keys [channel-id author] :as context} emote message-id]
  (when-let [index (-> emote emote->dir-tictactoe dir->index)]
    (update-game! "tictactoe" (merge state context) index)
    (m/delete-user-reaction! messaging channel-id message-id emote author)))

;; --- general ---

(defmethod execute! "quit" [{:keys [messaging]} {:keys [author channel-id]} msg-content]
  (let [games #{"wordle" "2048" "tictactoe"}
        game  (command-arg-text msg-content)]
    (if (games game)
      (do (quit-game! game author  channel-id)
          (m/create-message! messaging channel-id :content (str "Quitting " game)))
      (m/create-message! messaging channel-id :content (str "No game called " game)))))

(defmethod execute! :default [_ _ _])
(defmethod react! :default [_ _ _ _])
