(ns awesome.clara.examples
  (:require [clara.rules :refer :all])
  (:refer-clojure :exclude [name])
  (:import (com.github.zjjfly.awesome User)))


(defrecord SupportRequest [client level])

(defrecord ClientRepresentative [name client])

(defrule is-important
  "Find important support requests."
  [SupportRequest (= level :high)]
  =>
  (println "High support requested!"))

(defrule notify-client-rep
  "Find the client representative and send a notification of a support request."
  [SupportRequest (= ?client client)]
  [ClientRepresentative (= ?client client) (= ?name name)]  ; Join via the ?client binding.
  =>
  (println "Notify" ?name "that" ?client "has a new support request!"))

;使用java bean
(defrule user
  ;fact表达式中的变量绑定
  [User (= ?name name) (= ?age age)]
  ;使用tests定义规则的触发条件,可以使用任意的返回boolean类型的clojure代码
  [:test (and (not (nil? (re-seq #"A.*" ?name)))
              (> ?age 10))]
  =>
  (println "user valid!"))


(defrule animal-rule
  [:or                                                      ;boolean表达式,一种应用于fact表达式之上的前缀布尔表达式,前缀可以是:or或:and
   [:cat                                                    ;fact type
    [{age :age}]                                            ;解构
    (< age 10) (= ?age age)]                                ;fact表达式中的条件表达式
   [:dog                                                    ;fact type
    [{age :age}]
    (> age 3) (= ?age age)]
   ]
  =>
  (println (str "age:" ?age)))

(-> (mk-session :fact-type-fn :type)                        ;:fact-type-fn和factor type搭配使用,它的值应该是一个函数,返回的值对应了相关的fact type,默认值是type函数
    (insert {:type :cat :name "Jack" :age 2})
    (insert {:type :dog :name "Mike" :age 1})
    (fire-rules))

;; Run the rules! We can just use Clojure's threading macro to wire things up.
(-> (mk-session)
    (insert (->ClientRepresentative "Alice" "Acme")
            (->SupportRequest "Acme" :high)
            (User. "Acme" 11))
    (fire-rules))

