(ns c3)

(defn pfilter
  ([pred coll] (pfilter pred coll {:block-size 64 :parallelism 4}))
  ([pred coll {:keys [block-size parallelism]
               :or {block-size 64, parallelism 4}}]
   (let [par (max 1 (int parallelism))
         bsz (max 1 (int block-size))
         blocks (partition-all bsz coll)
         make-fut (fn [blk] (future (doall (filter pred blk))))
         init-futs (vec (map make-fut (take par blocks)))
         remain    (drop par blocks)]
     (letfn [(step [futs blks]
               (lazy-seq
                 (when (seq futs)
                   (let [head     (first futs)
                         tail     (subvec futs 1)
                         [tail blks]
                         (if-let [b (first blks)]
                           [(conj tail (make-fut b)) (rest blks)]
                           [tail blks])
                         res @head]
                     (if (seq res)
                       (concat res (step tail blks))
                       (step tail blks))))))]
       (step init-futs remain)))))

(defn -main [& args]
  (let [n (try (Integer/parseInt (first args)) (catch Exception _ 100000))
        par (try (Integer/parseInt (second args)) (catch Exception _ 4))
        bs  (try (Integer/parseInt (nth args 2)) (catch Exception _ 1024))
        xs (range n)
        pred #(zero? (mod % 3))]
    (let [t1 (System/nanoTime)
          a1 (count (filter pred xs))
          t2 (System/nanoTime)
          a2 (count (pfilter pred xs {:parallelism par :block-size bs}))
          t3 (System/nanoTime)]
      (println "serial count:" a1 "time ms:" (/ (- t2 t1) 1e6))
      (println "pfilter count:" a2 "time ms:" (/ (- t3 t2) 1e6))
      (println "first 20 from infinite:" (take 20 (pfilter odd? (range)))))
    (shutdown-agents)))
