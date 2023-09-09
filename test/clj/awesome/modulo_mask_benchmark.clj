(ns awesome.modulo-mask-benchmark
  (:require [criterium.core :as criterium]
            [primitive-math :as primitive-math]))

;refer to this blog: http://psy-lob-saw.blogspot.com/2014/11/the-mythical-modulo-mask.html

(primitive-math/use-primitive-operators)

(defn opt-modulo
  ^long [^long x ^long y]
  (bit-and x (- y 1)))

(opt-modulo 7 4)

(let [mean (first (:mean (criterium/benchmark* #(rem 7 4) {})))]
  (println (str "mean time of %: " mean)))

(let [mean (first (:mean (criterium/benchmark* #(opt-modulo 7 4) {})))]
  (println (str "mean time of opt-modulo: " mean)))
