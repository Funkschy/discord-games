(ns discord-games.wordle-test
  (:require [discord-games.wordle :refer [calculate-hits]]
            [clojure.test :refer [deftest testing is]]))

(deftest wordle
  (testing "calculate-hits"
    (is (= (calculate-hits "umami" "mommy")
           [:in-word :miss :miss :correct :miss]))
    (is (= (calculate-hits "mommy" "umami")
           [:miss :in-word :miss :correct :miss]))
    (is (= (calculate-hits "teats" "stats")
           [:miss :in-word :correct :correct :correct]))
    (is (= (calculate-hits "stats" "teats")
           [:in-word :miss :correct :correct :correct]))
    (is (= (calculate-hits "sands" "salad")
           [:correct :correct :miss :miss :in-word]))
    (is (= (calculate-hits "salad" "sands")
           [:correct :correct :miss :in-word :miss]))
    (is (= (calculate-hits "salad" "areas")
           [:in-word :miss :miss :correct :in-word]))
    (is (= (calculate-hits "areas" "salad")
           [:in-word :in-word :miss :correct :miss]))))
