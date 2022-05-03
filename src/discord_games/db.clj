(ns discord-games.db
  (:require
   [clojure.tools.logging :as log]
   [discord-games.config :refer [config]]
   [clojure.string :as str])
  (:import
   [java.sql DriverManager PreparedStatement Connection]))

(def connection
  (delay
   (log/info "creating database connection")
   (let [c (DriverManager/getConnection "jdbc:sqlite:bot.db")
         s (.createStatement c)]
     (.setQueryTimeout s 30)
     (.executeUpdate s (slurp "init.sql"))
     c)))

(defn disconnect! []
  (.close ^Connection @connection))

(defprotocol SetInStatement
  (set-value [value stmt idx]))

(extend-protocol SetInStatement
  java.lang.Long
  (set-value [value stmt idx]
    (.setLong ^PreparedStatement stmt idx value))
  java.lang.String
  (set-value [value stmt idx]
    (.setString ^PreparedStatement stmt idx value)))

(defn- args-string [args]
  (if (get-in config [:log :show-database-values])
    (str "args: [" (str/join ", " (map pr-str args)) "]")
    (str (count args) "args")))

(defn- exec [statement args execute]
  (let [s (.prepareStatement ^Connection @connection statement)]
    (try
      (log/info "Executing" statement "with" (args-string args))
      (doseq [[i a] (map vector (range) args)]
        (set-value a s (inc i)))
      (execute s)
      (finally (.close s)))))

(defn exec-query [statement & args]
  (exec statement args (fn [^PreparedStatement s] (resultset-seq (.executeQuery s)))))

(defn exec-update [statement & args]
  (exec statement args (fn [^PreparedStatement s] (.executeUpdate s))))
