(ns awesome.malli.benchmark
  (:require [clojure.spec.alpha :as s]
            [criterium.core :as cc]
            [malli.core :as m]))

;虽然Malli已经对序列正则做了很多优化
(comment
  (let [valid? (partial s/valid? (s/* int?))]
    (cc/quick-bench (valid? (range 10))))
  )
;Execution time mean : 12.375669 µs

(comment
  (let [valid? (m/validator [:* int?])]
    (cc/quick-bench (valid? (range 10))))
  )
;Execution time mean : 1.312336 µs

;但是尽量使用专用的schema，它们的性能往往更好
(comment
  (let [valid? (partial s/valid? (s/coll-of int?))]
    (cc/quick-bench (valid? (range 10))))
  )
;Execution time mean : 645.553402 ns

(comment
  (let [valid? (m/validator [:sequential int?])]
    (cc/quick-bench (valid? (range 10))))
  )
;Execution time mean : 6.260819 ns
