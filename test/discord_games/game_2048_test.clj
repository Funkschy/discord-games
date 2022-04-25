(ns discord-games.game-2048-test
  (:require
   [discord-games.game-2048 :refer [shift]]
   [clojure.test :refer [deftest testing is]]))

(deftest shifting
  (testing "left"
    (testing "single number"
      (is (= (:board (shift {:board [0 0 0 2 0 0 0 0 0 0 0 0 0 0 0 0]} :left))
             [2 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0]))
      (is (= (:board (shift {:board [0 0 2 0 0 0 0 0 0 0 0 0 0 0 0 0]} :left))
             [2 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0]))
      (is (= (:board (shift {:board [0 2 0 0 0 0 0 0 0 0 0 0 0 0 0 0]} :left))
             [2 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0]))
      (is (= (:board (shift {:board [2 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0]} :left))
             [2 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0])))
    (testing "two numbers"
      (is (= (:board (shift {:board [0 0 2 2 0 0 0 0 0 0 0 0 0 0 0 0]} :left))
             [4 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0]))
      (is (= (:board (shift {:board [2 0 2 0 0 0 0 0 0 0 0 0 0 0 0 0]} :left))
             [4 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0]))
      (is (= (:board (shift {:board [0 2 2 0 0 0 0 0 0 0 0 0 0 0 0 0]} :left))
             [4 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0]))
      (is (= (:board (shift {:board [2 0 0 2 0 0 0 0 0 0 0 0 0 0 0 0]} :left))
             [4 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0])))
    (testing "3 numbers"
      (is (= (:board (shift {:board [0 2 2 2 0 0 0 0 0 0 0 0 0 0 0 0]} :left))
             [4 2 0 0 0 0 0 0 0 0 0 0 0 0 0 0]))
      (is (= (:board (shift {:board [2 0 2 2 0 0 0 0 0 0 0 0 0 0 0 0]} :left))
             [4 2 0 0 0 0 0 0 0 0 0 0 0 0 0 0]))
      (is (= (:board (shift {:board [2 2 0 2 0 0 0 0 0 0 0 0 0 0 0 0]} :left))
             [4 2 0 0 0 0 0 0 0 0 0 0 0 0 0 0]))
      (is (= (:board (shift {:board [2 2 2 0 0 0 0 0 0 0 0 0 0 0 0 0]} :left))
             [4 2 0 0 0 0 0 0 0 0 0 0 0 0 0 0])))
    (testing "4 numbers"
      (is (= (:board (shift {:board [2 2 2 2 0 0 0 0 0 0 0 0 0 0 0 0]} :left))
             [4 4 0 0 0 0 0 0 0 0 0 0 0 0 0 0]))
      (is (= (:board (shift {:board [4 4 4 4 0 0 0 0 0 0 0 0 0 0 0 0]} :left))
             [8 8 0 0 0 0 0 0 0 0 0 0 0 0 0 0]))
      (is (= (:board (shift {:board [2 2 4 4 0 0 0 0 0 0 0 0 0 0 0 0]} :left))
             [4 8 0 0 0 0 0 0 0 0 0 0 0 0 0 0]))
      (is (= (:board (shift {:board [4 2 2 4 0 0 0 0 0 0 0 0 0 0 0 0]} :left))
             [4 4 4 0 0 0 0 0 0 0 0 0 0 0 0 0]))))
  (testing "right"
    (testing "single number"
      (is (= (:board (shift {:board [0 0 0 2 0 0 0 0 0 0 0 0 0 0 0 0]} :right))
             [0 0 0 2 0 0 0 0 0 0 0 0 0 0 0 0]))
      (is (= (:board (shift {:board [0 0 2 0 0 0 0 0 0 0 0 0 0 0 0 0]} :right))
             [0 0 0 2 0 0 0 0 0 0 0 0 0 0 0 0]))
      (is (= (:board (shift {:board [0 2 0 0 0 0 0 0 0 0 0 0 0 0 0 0]} :right))
             [0 0 0 2 0 0 0 0 0 0 0 0 0 0 0 0]))
      (is (= (:board (shift {:board [2 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0]} :right))
             [0 0 0 2 0 0 0 0 0 0 0 0 0 0 0 0])))
    (testing "two numbers"
      (is (= (:board (shift {:board [0 0 2 2 0 0 0 0 0 0 0 0 0 0 0 0]} :right))
             [0 0 0 4 0 0 0 0 0 0 0 0 0 0 0 0]))
      (is (= (:board (shift {:board [2 0 2 0 0 0 0 0 0 0 0 0 0 0 0 0]} :right))
             [0 0 0 4 0 0 0 0 0 0 0 0 0 0 0 0]))
      (is (= (:board (shift {:board [0 2 2 0 0 0 0 0 0 0 0 0 0 0 0 0]} :right))
             [0 0 0 4 0 0 0 0 0 0 0 0 0 0 0 0]))
      (is (= (:board (shift {:board [2 0 0 2 0 0 0 0 0 0 0 0 0 0 0 0]} :right))
             [0 0 0 4 0 0 0 0 0 0 0 0 0 0 0 0])))
    (testing "3 numbers"
      (is (= (:board (shift {:board [0 2 2 2 0 0 0 0 0 0 0 0 0 0 0 0]} :right))
             [0 0 2 4 0 0 0 0 0 0 0 0 0 0 0 0]))
      (is (= (:board (shift {:board [2 0 2 2 0 0 0 0 0 0 0 0 0 0 0 0]} :right))
             [0 0 2 4 0 0 0 0 0 0 0 0 0 0 0 0]))
      (is (= (:board (shift {:board [2 2 0 2 0 0 0 0 0 0 0 0 0 0 0 0]} :right))
             [0 0 2 4 0 0 0 0 0 0 0 0 0 0 0 0]))
      (is (= (:board (shift {:board [2 2 2 0 0 0 0 0 0 0 0 0 0 0 0 0]} :right))
             [0 0 2 4 0 0 0 0 0 0 0 0 0 0 0 0])))
    (testing "4 numbers"
      (is (= (:board (shift {:board [2 2 2 2 0 0 0 0 0 0 0 0 0 0 0 0]} :right))
             [0 0 4 4 0 0 0 0 0 0 0 0 0 0 0 0]))
      (is (= (:board (shift {:board [4 4 4 4 0 0 0 0 0 0 0 0 0 0 0 0]} :right))
             [0 0 8 8 0 0 0 0 0 0 0 0 0 0 0 0]))
      (is (= (:board (shift {:board [2 2 4 4 0 0 0 0 0 0 0 0 0 0 0 0]} :right))
             [0 0 4 8 0 0 0 0 0 0 0 0 0 0 0 0]))
      (is (= (:board (shift {:board [4 2 2 4 0 0 0 0 0 0 0 0 0 0 0 0]} :right))
             [0 4 4 4 0 0 0 0 0 0 0 0 0 0 0 0]))))
  (testing "up"
    (is (= (:board (shift {:board [0 2 0 0 0 2 0 0 0 2 0 0 0 0 0 0]} :up))
           [0 4 0 0 0 2 0 0 0 0 0 0 0 0 0 0])))
  (testing "down"
    (is (= (:board (shift {:board [0 2 0 0 0 2 0 0 0 2 0 0 0 0 0 0]} :down))
           [0 0 0 0 0 0 0 0 0 2 0 0 0 4 0 0]))))
