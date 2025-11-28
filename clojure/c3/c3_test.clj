(ns c3-test
  (:require [clojure.test :refer :all]
            [c3 :refer [pfilter]]))

(deftest finite-correctness
  (let [xs (range 1000)
        pred #(zero? (mod % 7))]
    (is (= (vec (filter pred xs))
           (vec (pfilter pred xs))))
    (is (= (vec (filter pred xs))
           (vec (pfilter pred xs {:block-size 13 :parallelism 3}))))))

(deftest lazy-infinite
  (let [res (take 20 (pfilter odd? (range)))]
    (is (= (take 20 (filter odd? (range))) res))))
