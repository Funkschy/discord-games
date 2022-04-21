(ns discord-games.state)

(def running-games (atom {}))

(defn running-game [user game]
  (get-in @running-games [user game]))

(defn message-game [user message-id]
  (get-in @running-games [user :messages message-id]))

(defn has-running-game? [user game]
  (boolean (running-game user game)))

(defn create-game! [user game init-state]
  (when (has-running-game? user game)
    (throw (IllegalStateException. (str user " already has a running game"))))
  (swap! running-games assoc-in [user game] init-state))

(defn create-reactable-message! [user message-id game]
  (swap! running-games assoc-in [user :messages message-id] game))

(defn quit-game! [user game]
  (swap! running-games update user #(dissoc % game)))

(defn update-game! [user game f & args]
  (apply swap! running-games update-in [user game] f args))
