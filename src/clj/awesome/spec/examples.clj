(ns awesome.spec.examples
  (:require [clojure.spec.alpha :as spec]
            [clojure.spec.gen.alpha :as gen]
            [clojure.spec.test.alpha :as stest]))

;定义一个map spec,其中的key都是和当前namespace绑定的
(def m-spec (spec/keys :req [::x ::y (or ::secret (and ::user ::pwd))]
                       :opt [::z]))
;调用conform,使用这个spec来验证某个map的正确性,返回的是map
;conform常用于实现解析和解构
(spec/conform m-spec {::x 1 ::y 2 ::user "jjzi" ::pwd "123456"})

;定义一个在验证和生成的时候会忽略key的namespace的spec
(def m-spec-un (spec/keys :req-un [::a ::b]))

(spec/conform m-spec-un {:a 1 :b 2 :c 3})

;sequence spec,需要一些正则操作来把多个Predicate组合起来
;定义多个Predicate
;一个针对奇数的spec
(spec/def ::even? (spec/and integer? even?))
;一个针对偶数的spec
(spec/def ::odd? (spec/and integer? odd?))
;几个整数的spec
(spec/def ::a integer?)
(spec/def ::b integer?)
(spec/def ::c integer?)
;cat把多个Predicate串连起来,这样就变成了一个针对vector的spec.
;它调用conform返回一个map,它的key是这些Predicate的key,key的值是对应的Predicate调用conform的结果
(def s (spec/cat :forty-two #{42}
                 ;+表示一个predicate可以出现一次或多次,conform的结果是一个vector
                 :odds (spec/+ ::odd?)
                 :m (spec/keys :req-un [::a ::b ::c])
                 ;*表示一个key predicate可以不出现或出现若干次,conform的结果是一个vector
                 :oes (spec/* (spec/cat :o ::odd? :e ::even?))
                 ;alt表示多个key predicate中的选择一个,conform的结果是第一个map entry,key是第一个匹配的Predicate的key,value是这个Predicate调用conform的结果
                 :ex (spec/alt :odd ::odd? :even ::even?)))
(spec/conform s [42 11 13 15 {:a 1 :b 2 :c 3} 1 2 3 42 43 44 11])
;{:forty-two 42,
; :odds [11 13 15],
; :m {:a 1, :b 2, :c 3},
; :oes [{:o 1, :e 2} {:o 3, :e 42} {:o 43, :e 44}],
; :ex [:odd 11]}

;explain可以解释某个值不匹配spec的原因
(spec/explain s [42 11 13 15 {:a 1 :b 2} 1 2 3 42 43 44 11])
;{:a 1, :b 2} - failed: (contains? % :c) in: [4] at: [:m]

;常用的定义spec的除了spec/keys,spec/def和正则操作之外,还有spec/and,spec/or,以及spec/spec,最后的这种只有当需要重载generator的时候使用
;这些都可以直接使用predicate函数或set
(spec/conform (spec/and int? pos?) 1)
;1


;spec可以为函数定义spec,使用spec/fdef
(defn ranged-rand
  "Returns random int in range start <= rand < end"
  [start end]
  (+ start (long (rand (- end start)))))

(spec/fdef ranged-rand
           :args (spec/and (spec/cat :start int? :end int?)
                           #(< (:start %) (:end %)))
           :ret int?
           :fn (spec/and #(>= (:ret %) (-> % :args :start))
                         #(< (:ret %) (-> % :args :end))))

;可以使用instrument对现有的函数进行修改，使得在调用它的时候执行相关的spec
(stest/instrument `ranged-rand)
(comment
  (ranged-rand 1 -10)
  )

;使用gen/sample来随机生成一个符合spec的值,常用于测试
(gen/sample (spec/gen s))

;使用valid?可以验证一个值是否符合某个spec
(spec/valid? (spec/and int? pos?) 1)

;一些使用aero的最佳实践:
;1.把密码放在单独的文件中,用#include引入
;2.把读取配置的代码放在单独的ns中,并简单的做一层封装
;3.可以和Plumatic Schema这样的格式校验工具一起使用
;4.最好使用单个配置文件
