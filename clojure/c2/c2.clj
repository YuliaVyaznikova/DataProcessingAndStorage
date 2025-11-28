(ns c2)

(defn primes
  []
  (letfn [(step [candidate composites]
            (lazy-seq
              (if-let [factors (get composites candidate)]
                (let [next-composites (reduce (fn [m p]
                                                (let [n (+ candidate p)]
                                                  (update m n (fnil conj []) p)))
                                              (dissoc composites candidate)
                                              factors)]
                  (step (inc candidate) next-composites))
                (cons candidate
                      (step (inc candidate)
                            (if (>= candidate 2)
                              (update composites (* candidate candidate) (fnil conj []) candidate)
                              composites))))))]
    (step 2 {})))

(defn -main [& args]
  (let [n (try (Integer/parseInt (first args)) (catch Exception _ 10))
        xs (take n (primes))]
    (println (vec xs))))