(ns awesome.meander.examples
  (:require [meander.epsilon :as m]
            [meander.strategy.epsilon :as m*]))

;Meander主要用于较复杂的数据结构之间的转换

;match方法是Meander中基础的一个模式匹配的宏，其中的?开头的变量叫作logic variable，用于匹配一个任意的值
(defn favorite-food-info [foods-by-name user]
  (m/match {:user          user
            :foods-by-name foods-by-name}
           {:user
            {:name          ?name
             :favorite-food {:name ?food}}
            :foods-by-name {?food {:popularity ?popularity
                                   :calories   ?calories}}}
           {:name     ?name
            :favorite {:food       ?food
                       :popularity ?popularity
                       :calories   ?calories}}))

(def foods-by-name
  {:nachos   {:popularity :high
              :calories   :lots}
   :smoothie {:popularity :high
              :calories   :less}})

(favorite-food-info foods-by-name
                    {:name          :alice
                     :favorite-food {:name :nachos}})
;{:name :alice, :favorite {:food :nachos, :popularity :high, :calories :lots}}

;如果一个user有多个favorite-food，则需要使用search这个宏
(defn favorite-foods-info [foods-by-name user]
  (m/search {:user          user
             :foods-by-name foods-by-name}
            {:user
             {:name           ?name
              :favorite-foods (m/scan {:name ?food})}
             :foods-by-name {?food {:popularity ?popularity
                                    :calories   ?calories}}}
            {:name     ?name
             :favorite {:food       ?food
                        :popularity ?popularity
                        :calories   ?calories}}))


(favorite-foods-info foods-by-name
                     {:name           :alice
                      :favorite-foods [{:name :nachos}
                                       {:name :smoothie}]})
;({:name :alice, :favorite {:food :nachos, :popularity :high, :calories :lots}}
; {:name :alice, :favorite {:food :smoothie, :popularity :high, :calories :less}})

;如果user的favorite foods分布在不同的地方，我们想要把它们收集到一个变量中，那么需要使用memory variable，它以!开头
;find方法类似search，但只返回第一个匹配项
(defn grab-all-foods [user]
  (m/find user
          {:favorite-foods [{:name !foods} ...]
           :special-food   !foods
           :recipes        [{:title !foods} ...]
           :meal-plan      {:breakfast [{:food !foods} ...]
                            :lunch     [{:food !foods} ...]
                            :dinner    [{:food !foods} ...]}}
          !foods))

(grab-all-foods {
                 :favorite-foods [{:name "Nachos"} {:name "Smoothie"}]
                 :special-food   "Foo"
                 :recipes        [{:title "X"} {:title "Y"}]
                 :meal-plan      {:breakfast [{:food "Bar"}]
                                  :lunch     [{:food "A"}]
                                  :dinner    [{:food "B"}]}
                 })
;["Nachos" "Smoothie" "Foo" "X" "Y" "Bar" "A" "B"]

;Meander也提供了传统的模式匹配功能
(def point [1 2])

;find可以有多个匹配项
(m/find point
        [?x ?y] ?y
        [?x ?y ?z] ?y)
;2
;上面的模式中，?x，?z实际没有被使用，所以可以使用wildcard来代替
;在Meander中，wildcard是以_开头的symbol

;Meander可以使用predicate函数来限定值的类型或范围
(m/find point
        [(m/pred number?) (m/pred number? ?y)]
        ?y

        [(m/pred number?) (m/pred number? ?y) (m/pred number?)]
        ?y)
;2

;上面的代码有一点冗长，我们可以自己实现一个Meander的扩展来简化代码
(m/defsyntax number
             ([] `(number _#))
             ([pattern]
              (if (m/match-syntax? &env)
                `(m/pred number? ~pattern)
                &form)))

(m/find point
        [(number) (number ?y)]
        ?y

        [(number) (number ?y) (number)]
        ?y)
;2

;有时候对于数据的转换需要多个步骤，Meander的strategies对应的就是这种需求
(def eliminate-zeros
  (m*/rewrite
    (+ ?x 0) ?x
    (+ 0 ?x) ?x))

;bottom-up表示从最里层开始应用strategy，attempt表示尝试应用这个strategy，如果不成功则返回传入的值
(def eliminate-all-zeros
  (m*/bottom-up
    (m*/attempt eliminate-zeros)))

(eliminate-all-zeros '(+ (+ 0 (+ 0 (+ 3 (+ 2 0)))) 0))
;(+ 3 2)
