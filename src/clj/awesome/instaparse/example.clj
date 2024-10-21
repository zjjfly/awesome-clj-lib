(ns awesome.instaparse.example
  (:require [clojure.java.io :as io]
            [instaparse.core :as ip]
            [instaparse.transform :as it]))

(def calc-parser
  (ip/parser
    (slurp (io/resource "calculator.bnf"))))

(defn calculate
  [string]
  (it/transform
    {:START  identity
     :NUMBER #(Integer/parseInt %)
     :SUM    +
     :DIFF   -
     :PROD   *
     :DIV    /}
    (let [tree (calc-parser string)]
      (tap> tree)
      tree)))

(defn -main
  [& _args]
  (loop [line (read-line)]
    (when line
      (println (calculate line))
      (recur (read-line))))
  (System/exit 0))

(calculate "1 + 2 * 3")
