(ns discord-games.core
  (:gen-class)
  (:require
   [clojure.core.async :as a]
   [clojure.tools.logging]
   [discljord.connections :as c]
   [discljord.messaging :as m]
   [discord-games.commands :refer [command-prefix execute! react!]]
   [discord-games.logging]))

(def token (System/getenv "DISCORD_GAMES_TOKEN"))
(def intents #{:guilds :guild-messages :guild-message-reactions})

(defmulti handle-event
  (fn [event-type event-data state]
    event-type))

(defmethod handle-event :default
  [_ _ state]
  state)

(defmethod handle-event :message-create
  [_ {{:keys [bot id]} :author :keys [channel-id content]} state]
  (if (= content (str command-prefix "disconnect"))
    (do (c/disconnect-bot! (:connection state))
        (assoc state :disconnected true))
    (do (when-not bot
          (execute! state {:author id :channel-id channel-id} content))
        state)))

(defmethod handle-event :message-reaction-add
  [_ {:keys [message-id channel-id] :as event-data} state]
  (let [{:keys [id bot]} (get-in event-data [:member :user])
        emote   (get-in event-data [:emoji :name])]
    (when-not bot
      (react! state {:author id :channel-id channel-id} emote message-id))))

(defn -main  []
  (let [event-ch (a/chan 100)
        connection-ch (c/connect-bot! token event-ch :intents intents)
        messaging-ch  (m/start-connection! token)
        init-state {:connection connection-ch
                    :event event-ch
                    :messaging messaging-ch}]
    (try (loop []
           (let [[event-type event-data] (a/<!! event-ch)]
             (handle-event event-type event-data init-state)
             (recur)))
         (finally
           (m/stop-connection! messaging-ch)
           (a/close!           event-ch)))))
