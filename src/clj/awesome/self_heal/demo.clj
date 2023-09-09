;demo show the idea about function self-healing on [WORKS ON MY MACHINE: SELF HEALING CODE WITH CLOJURE.SPEC](https://cognitect.com/blog/2016/12/9/works-on-my-machine-self-healing-code-with-clojurespec-1.html)
(ns awesome.self-heal.demo
  (:require [clojure.spec.alpha :as s]
            [clojure.spec.gen.alpha :as gen]
            [clojure.string :as string]
            [awesome.self-heal.candidates]))


(declare clean-bad-data)
(declare calc-average)
(declare display-report)

(defn report [earnings]
  (-> earnings
      (clean-bad-data)
      (calc-average)
      (display-report)))

(defn clean-bad-data [earnings]
  (filter number? earnings))

(s/def ::earnings (s/cat :elements (s/coll-of any?)))

(s/def ::cleaned-earnings (s/with-gen
                            (s/cat :clean-elements (s/coll-of number?))
                            #(gen/return [[1 2 3 4 5]])))

(s/exercise ::cleaned-earnings 1)

(s/fdef clean-bad-data
        :args ::earnings
        :ret ::cleaned-earnings)

(defn calc-average [earnings]
  (/ (apply + earnings) (count earnings)))

(s/def ::average number?)

(s/fdef calc-average
        :args ::cleaned-earnings
        :ret ::average)

(s/def ::report-format string?)

(defn display-report [avg]
  (str "The average is " avg))

(s/fdef display-report
        :args (s/cat :elements ::average)
        :ret ::report-format)

(s/fdef report
        :args ::earnings
        :ret ::report-format)

(report [1 2 3 4 5])

(comment
  (report []))

(s/def ::numbers (s/cat :elements (s/coll-of number?)))
(s/def ::result number?)

(defn better-calc-average [earnings]
  (if (empty? earnings)
    0
    (/ (apply + earnings) (count earnings))))

(defn get-spec-data [spec-symb]
  (let [[_ _ args _ ret _ fn] (s/form spec-symb)]
    {:args args
     :ret  ret
     :fn   fn}))

(defn failing-function-name [e]
  (as-> (.getStackTrace e) ?
        (map #(.getClassName %) ?)
        (filter #(string/starts-with? % "awesome.self_heal.demo") ?)
        (first ?)
        (string/split ? #"\$")
        (last ?)
        (string/replace ? #"_" "-")
        (str *ns* "/" ?)))

(defn spec-inputs-match? [args1 args2 input]
  (println "****Comparing args" args1 args2 "with input" input)
  (and (s/valid? args1 input)
       (s/valid? args2 input)))

(defn- try-fn [f input]
  (try (apply f input) (catch Exception e :failed)))

(defn spec-return-match? [fname c-fspec orig-fspec failing-input candidate]
  (let [rcandidate (resolve candidate)
        orig-fn (resolve (symbol fname))
        result-new (try-fn rcandidate failing-input)
        [[seed]] (s/exercise (:args orig-fspec) 1)
        result-old-seed (try-fn rcandidate seed)
        result-new-seed (try-fn orig-fn seed)]
    (println "****Comparing seed " seed "with new function")
    (println "****Result: old" result-old-seed "new" result-new-seed)
    (and (not= :failed result-new)
         (s/valid? (:ret c-fspec) result-new)
         (s/valid? (:ret orig-fspec) result-new)
         (= result-old-seed result-new-seed))))

(defn spec-matching? [fname orig-fspec failing-input candidate]
  (println "----------")
  (println "**Looking at candidate " candidate)
  (let [c-fspec (get-spec-data candidate)]
    (and (spec-inputs-match? (:args c-fspec) (:args orig-fspec) failing-input)
         (spec-return-match? fname c-fspec orig-fspec failing-input candidate))))

(defn find-spec-candidate-match [fname fspec-data failing-input]
  (let [candidates (->> (s/registry)
                        keys
                        (filter #(string/starts-with? (namespace %) "awesome.self-heal.candidates"))
                        (filter symbol?))]
    (println "Checking candidates " candidates)
    (some #(if (spec-matching? fname fspec-data failing-input %) %) (shuffle candidates))))


(defn self-heal [e input orig-form]
  (let [fname (failing-function-name e)
        _ (println "ERROR in function" fname (.getMessage e) "-- looking for replacement")
        fspec-data (get-spec-data (symbol fname))
        _ (println "Retriving spec information for function " fspec-data)
        match (find-spec-candidate-match fname fspec-data [input])]
    (if match
      (do
        (println "Found a matching candidate replacement for failing function" fname " for input" input)
        (println "Replacing with candidate match" match)
        (println "----------")
        (eval `(def ~(symbol fname) ~match))
        (println "Calling function again")
        (let [new-result (eval orig-form)]
          (println "Healed function result is:" new-result)
          new-result))
      (println "No suitable replacment for failing function " fname " with input " input ":("))))

(defmacro with-healing [body]
  (let [params (second body)]
    `(try ~body
          (catch Exception e# (self-heal e# ~params '~body)))))


(with-healing (report [1 2 3 4 5 "a" "b"]))

(with-healing (report []))
;ERROR in function awesome.self-heal.demo/calc-average Divide by zero -- looking for replacement
;Retriving spec information for function  {:args :awesome.self-heal.demo/cleaned-earnings, :ret :awesome.self-heal.demo/average, :fn nil}
;Checking candidates  (awesome.self-heal.candidates/better-calc-average)
;----------
;**Looking at candidate  awesome.self-heal.candidates/better-calc-average
;****Comparing args :awesome.self-heal.candidates/numbers :awesome.self-heal.demo/cleaned-earnings with input [[]]
;****Comparing seed  [[1 2 3 4 5]] with new function
;****Result: old 3 new 3
;Found a matching candidate replacement for failing function awesome.self-heal.demo/calc-average  for input []
;Replacing with candidate match awesome.self-heal.candidates/better-calc-average
;----------
;Calling function again
;Healed function result is: The average is 0
