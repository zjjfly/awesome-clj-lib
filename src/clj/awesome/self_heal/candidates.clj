(ns awesome.self-heal.candidates
  (:require [clojure.spec.alpha :as s]))

(s/def ::numbers (s/cat :elements (s/coll-of number?)))
(s/def ::result number?)

(defn better-calc-average [earnings]
  (if (empty? earnings)
    0
    (/ (apply + earnings) (count earnings))))

(s/fdef better-calc-average
        :args ::numbers
        :ret ::result)
