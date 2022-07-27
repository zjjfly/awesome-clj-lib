(ns com.github.zjjfly.awesome.core.async.go-thread
  (:require [clojure.test :refer :all]
            [com.github.zjjfly.awesome.core.async.examples :refer [async-go-factorizer
                                                                   async-thread-factorizer
                                                                   async-with-pipeline
                                                                   launch-go-blocking-and-compute
                                                                   launch-thread-blocking-and-compute]]
            [criterium.core :as criterium]))

(defn mean-time [f]
  (* 1000 (first (:mean (criterium/quick-benchmark* f {})))))

;使用core.async的go,thread,pipeline来并行调用factorizer,它们的性能应该是差不多的
(deftest factorizer-test
  (let [
        go-time (mean-time #(async-go-factorizer (repeat 1000 (* 29 982451653)) 8))
        thread-time (mean-time #(async-thread-factorizer (repeat 1000 (* 29 982451653)) 8))
        pipeline-time (mean-time #(async-with-pipeline (repeat 1000 (* 29 982451653)) 8))
        ]
    (is (< (Math/abs (- go-time thread-time)) 50))
    (is (< (Math/abs (- go-time pipeline-time)) 50))))

;go和thread在有阻塞操作的时候,thread的性能比较好的
(deftest block-test
  (let [
        go-time (mean-time #(launch-go-blocking-and-compute 32 16))
        thread-time (mean-time #(launch-thread-blocking-and-compute 32 16))
        ]
    (is (> (- go-time thread-time) 500))))

