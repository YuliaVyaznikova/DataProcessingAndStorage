(ns c2-test
  (:require [clojure.test :refer :all]
            [c2 :refer [primes]]))

(deftest first-ten-primes
  (is (= [2 3 5 7 11 13 17 19 23 29]
         (vec (take 10 (primes))))))

(deftest nth-prime
  (is (= 7927 (nth (primes) 1000))))

(deftest no-even-primes-except-two
  (let [xs (take 200 (primes))]
    (is (= 2 (first xs)))
    (is (every? odd? (rest xs)))))