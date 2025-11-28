(ns c1
  (:require [clojure.string :as str]))

(defn -main [& args]
  (let [[alphabet-arg n-arg] args
        alphabet (->> (str/split (or alphabet-arg "") #",")
                      (remove #(= % "")))
        n (Integer/parseInt (or n-arg "0"))
        step (fn [words _]
               (reduce (fn [acc w]
                         (let [lastch (when (pos? (count w))
                                        (subs w (dec (count w))))
                               choices (if lastch
                                         (filter #(not= % lastch) alphabet)
                                         alphabet)
                               extended (map #(str w %) choices)]
                           (concat acc extended)))
                       '()
                       words))
        result (if (<= n 0)
                 '("")
                 (reduce step '("") (range n)))]
    (println result)))
