(ns c5
  (:require [clojure.string :as str]))

(defn- fork [id]
  (ref {:id id :uses 0 :taken false :last-owner nil}))

(defn- order-forks
  [strategy idx left right]
  (case strategy
    :ordered (if (< (System/identityHashCode left)
                    (System/identityHashCode right))
               [left right]
               [right left])
    :left-right [left right]
    :right-left [right left]
    :alternating (if (even? idx) [left right] [right left])
    :random (if (zero? (rand-int 2)) [left right] [right left])
    (if (< (System/identityHashCode left)
           (System/identityHashCode right))
      [left right]
      [right left])))

(defn- acquire-forks!
  [{:keys [strategy restart-counter]} idx left right]
  (loop []
    (let [acq
          (dosync
            (let [[f s] (order-forks strategy idx left right)]
              (if (or (:taken @f) (:taken @s))
                :retry
                (do (alter f assoc :taken true :last-owner idx)
                    (alter s assoc :taken true :last-owner idx)
                    :ok))))]
      (if (= acq :ok)
        :ok
        (do (swap! restart-counter inc)
            (Thread/yield)
            (recur))))))

(defn- release-forks!
  [left right]
  (dosync
    (alter left  (fn [st] (-> st (assoc :taken false) (update :uses inc))))
    (alter right (fn [st] (-> st (assoc :taken false) (update :uses inc))))))

(defn- philosopher-loop
  [{:keys [idx iterations think-ms eat-ms forks restart-counter strategy]}]
  (let [n (count forks)
        left  (forks idx)
        right (forks (mod (inc idx) n))]
    (loop [rem iterations meals 0]
      (if (zero? rem)
        {:philosopher idx :meals meals}
        (do
          (when (pos? think-ms) (Thread/sleep think-ms))
          (acquire-forks! {:strategy strategy :restart-counter restart-counter}
                          idx left right)
          (when (pos? eat-ms) (Thread/sleep eat-ms))
          (release-forks! left right)
          (recur (dec rem) (inc meals)))))))

(defn simulation
  [{:keys [philosophers iterations think-ms eat-ms strategy warmup-ms timeout-ms]
    :or {philosophers 5 iterations 10 think-ms 5 eat-ms 5 strategy :ordered warmup-ms 0}}]
  (let [forks (vec (map fork (range philosophers)))
        restart-counter (atom 0)
        start-promise (promise)
        workers (mapv (fn [idx]
                        (future
                          @start-promise
                          (philosopher-loop {:idx idx :iterations iterations
                                             :think-ms think-ms :eat-ms eat-ms
                                             :forks forks :restart-counter restart-counter
                                             :strategy strategy})))
                      (range philosophers))
        t0 (System/nanoTime)]
    (when (pos? warmup-ms) (Thread/sleep warmup-ms))
    (deliver start-promise true)
    (let [deadline (when timeout-ms (+ (System/currentTimeMillis) timeout-ms))
          results (loop [ws workers acc []]
                    (if (empty? ws)
                      acc
                      (let [w (first ws)
                            wait (when deadline (max 1 (- deadline (System/currentTimeMillis))))
                            v (if timeout-ms (deref w wait ::timeout) (deref w))]
                        (if (= v ::timeout)
                          (do (doseq [x ws] (future-cancel x)) ::timeout)
                          (recur (rest ws) (conj acc v))))))
          dur-ms (/ (- (System/nanoTime) t0) 1e6)
          fork-uses (mapv (fn [r] (:uses @r)) forks)]
      (if (= results ::timeout)
        {:status :timeout :duration-ms dur-ms :restart-count @restart-counter
         :fork-usage fork-uses :philosophers []
         :config {:philosophers philosophers :iterations iterations :think-ms think-ms :eat-ms eat-ms :strategy strategy :timeout-ms timeout-ms}}
        (let [meals (vec results)
              total (reduce + (map :meals meals))]
          {:status :completed :duration-ms dur-ms :restart-count @restart-counter
           :fork-usage fork-uses :philosophers meals :total-meals total
           :config {:philosophers philosophers :iterations iterations :think-ms think-ms :eat-ms eat-ms :strategy strategy :timeout-ms timeout-ms}})))))

(defn summary
  [{:keys [status duration-ms restart-count fork-usage philosophers config]}]
  (str/join \newline
            [(format "Status: %s" (name status))
             (format "Duration: %.2f ms" duration-ms)
             (format "Transaction restarts: %d" restart-count)
             (format "Fork usage: %s" (pr-str fork-usage))
             (format "Meals per philosopher: %s" (pr-str (mapv :meals philosophers)))
             (format "Config: %s" (pr-str config))]))

(defn run-once
  [opts]
  (try
    (summary (simulation opts))
    (finally (shutdown-agents))))

(defn suite
  []
  (let [cases [{:label "odd"
                 :opts {:philosophers 5 :iterations 20 :think-ms 2 :eat-ms 2 :strategy :ordered}}
                {:label "even"
                 :opts {:philosophers 6 :iterations 20 :think-ms 2 :eat-ms 2 :strategy :alternating}}
                {:label "livelock"
                 :opts {:philosophers 6 :iterations 200 :think-ms 0 :eat-ms 2 :strategy :left-right :timeout-ms 300}}]]
    (mapv (fn [{:keys [label opts]}]
            {:label label :result (simulation opts)})
          cases)))

(defn suite-summary []
  (let [res (suite)]
    (str/join
      (str \newline "----" \newline)
      (map (fn [{:keys [label result]}]
             (str label "\n" (summary result)))
           res))))

(defn run-suite []
  (try
    (suite-summary)
    (finally (shutdown-agents))))

(defn -main [& _]
  (println (run-once {:philosophers 5 :iterations 20 :think-ms 2 :eat-ms 2 :strategy :ordered})))