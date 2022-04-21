(ns discord-games.wordle
  (:require
   [clojure.java.io :refer [resource]]
   [clojure.set :as sets]
   [clojure.string :as str]
   [discord-games.render :refer [draw-letter! TextTileGame]]))

(defn- read-word-set [init-coll filename]
  (into init-coll (str/split-lines (slurp (resource filename)))))

(def ^:private possible-solution-words (read-word-set [] "wordle-allowed.txt"))
(def ^:private guessable-words  (sets/union possible-solution-words (read-word-set #{} "wordle-all.txt")))

(defn- check-hit [in-actual a g]
  (cond
    (= a g)       :correct
    (in-actual g) :in-word
    :else         :miss))

(defn- freq-of [hits guess status]
  (->> (map vector hits guess)
       (filter #(= status (first %)))
       (map second)
       (frequencies)))

(defn- update-hits-with-budget [[budget :as acc] [status c]]
  (if (= status :in-word)
    (-> acc
        (update-in [0 c] dec)
        (update 1 conj (if (and (budget c) (> (budget c) 0)) :in-word :miss)))
    (update acc 1 conj status)))

(defn- calculate-hits [actual guess]
  (let [hits          (map (partial check-hit (set actual)) actual guess)
        yellow-budget (merge-with - (frequencies actual) (freq-of hits guess :correct))]
    (->> (map vector hits guess)
         (reduce update-hits-with-budget [yellow-budget []])
         (second))))

(defn- update-with-guess [{:keys [actual] :as state} guess]
  (let [hits   (calculate-hits actual guess)
        won?   (every? #{:correct} hits)
        state  (update state :tries conj [guess hits])
        status (if won? :won :playing)]
    (assoc state :status status)))

(defn- error [game-state message]
  (-> game-state
      (assoc :status :error)
      (assoc :message message)))

(defn- tries [{tries :tries}]
  (count tries))

(defn- tries-left [game-state]
  (- 6 (tries game-state)))

(defn- check-lost [game-state]
  (if (zero? (tries-left game-state))
    (-> game-state
        (assoc :status :lost)
        (assoc :message (str "The word was " (:actual game-state))))
    game-state))

;; -- API --

(def letter-size 128)

(defrecord Wordle [actual]
  TextTileGame
  (draw-game! [game-state g w h]
    (let [colors      {:miss "#3a3a3c" :in-word "#b59f3b" :correct "#538d4e"}
          letter-size (/ w 5)
          text-color  "#d7dadc"
          tries       (reverse (:tries game-state))]
      (assert (= (/ w 5) (/ h 6)) "invalid image size")
      (loop [[[guess results] & tail] tries, y 0]
        (doseq [[offset letter result] (map vector (range) guess results)]
          (draw-letter! g (* offset letter-size) y letter-size letter-size letter text-color (colors result)))
        (when tail
          (recur tail (long (+ y letter-size)))))))
  (dims [_]
    [(* 5 letter-size) (* 6 letter-size) letter-size]))

(defn new-game []
  (Wordle. (rand-nth possible-solution-words)))

(defn won? [{status :status}]
  (= status :won))

(defn lost? [{status :status}]
  (= status :lost))

(defn valid? [word]
  (contains? guessable-words word))

(defn make-guess [game-state guess]
  (let [guess (str/lower-case guess)]
    (cond
      (won? game-state)        game-state
      (lost? game-state)       game-state
      (> (tries game-state) 5) (error game-state "Game is already lost")
      (valid? guess)           (check-lost (update-with-guess game-state guess))
      :else (error game-state (str guess " is not a valid word")))))

(defn status-text [game-state]
  (case (:status game-state)
    :won   "Congratulations"
    :error (:message game-state)
    :lost  (:message game-state)
    (str (tries-left game-state) " tries left")))
