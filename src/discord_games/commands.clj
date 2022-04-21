(ns discord-games.commands
  (:require
   [clojure.string :as str]
   [discljord.messaging :as m]
   [discord-games.game-2048 :as g2048]
   [discord-games.render :as r]
   [discord-games.state :refer [create-game! create-reactable-message!
                                has-running-game? message-game quit-game!
                                running-game update-game!]]
   [discord-games.wordle :as wordle])
  (:import
   [java.io File]))

(def command-prefix "$")

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

(def img-channel 964619087332401244)

;; --- wordle ---

(defn- get-img-embed [messaging game file]
  (r/render-to-file! game file)
  (let [msg (m/create-message! messaging img-channel :attachments [file])
        url (get-in @msg [:attachments 0 :url])]
    {"title" "Game"
     "type"  "image"
     "image" {"url" url}}))

(defn- update-wordle-game! [messaging author channel-id guess]
  (let [{:keys [game file message]} (running-game author :wordle)
        updated (wordle/make-guess game guess)
        embed   (get-img-embed messaging updated file)]
    (update-game! author :wordle assoc :game updated)
    (m/edit-message! messaging
                     channel-id
                     message
                     :content (wordle/status-text updated)
                     :embed embed)))

(defn- start-wordle-game! [messaging author channel-id guess]
  (let [init-game (wordle/make-guess (wordle/new-game) guess)
        file      (game-image-file author)
        embed     (get-img-embed messaging init-game file)]
    (create-game! author :wordle {:game init-game :file file})
    (->> @(m/create-message! messaging
                             channel-id
                             :content (wordle/status-text init-game)
                             :embed embed)
         (:id)
         (update-game! author :wordle assoc :message))))

(defmethod execute! "wordle" [{:keys [messaging]} {:keys [author channel-id]} msg-content]
  (let [guess (command-arg-text msg-content)]
    (if (has-running-game? author :wordle)
      (update-wordle-game! messaging author channel-id guess)
      (start-wordle-game! messaging author channel-id guess))))

;; --- 2048 ---

(defn- update-2048-game! [messaging author channel-id dir]
  (let [{:keys [game file message]} (running-game author :2048)
        updated (g2048/move game dir)
        embed   (get-img-embed messaging updated file)]
    (update-game! author :2048 assoc :game updated)
    (m/edit-message! messaging
                     channel-id
                     message
                     :content (g2048/status-text updated)
                     :embed embed)))

(defn- init-reactions! [messaging channel-id author {id :id :as message}]
  (m/create-reaction! messaging channel-id id "⬅")
  (m/create-reaction! messaging channel-id id "➡")
  (m/create-reaction! messaging channel-id id "⬆")
  (m/create-reaction! messaging channel-id id "⬇")
  (create-reactable-message! author id :2048)
  message)

(defn- start-2048-game! [messaging author channel-id _]
  (let [init-game (g2048/new-game)
        file      (game-image-file author)
        embed     (get-img-embed messaging init-game file)]
    (create-game! author :2048 {:game init-game :file file})
    (->> @(m/create-message! messaging
                             channel-id
                             :content (g2048/status-text init-game)
                             :embed embed)
         (init-reactions! messaging channel-id author)
         (:id)
         (update-game! author :2048 assoc :message))))

(defmethod execute! "2048" [{:keys [messaging]} {:keys [author channel-id]} msg-content]
  (let [dir (command-arg-text msg-content)]
    (if (has-running-game? author :2048)
      (update-2048-game! messaging author channel-id dir)
      (start-2048-game! messaging author channel-id dir))))

(defmethod react! :2048 [{:keys [messaging]} {:keys [author channel-id]} emote message-id]
  (let [dirs {"⬅" "left"
              "➡" "right"
              "⬆"  "up"
              "⬇" "down"}
        dir (dirs emote)]
    (when dir
      (update-2048-game! messaging author channel-id dir)
      (m/delete-user-reaction! messaging
                               channel-id
                               message-id
                               emote
                               author))))

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
