(ns awesome.malli.examples
  (:require [malli.core :as m]
            [malli.experimental.lite :as l]
            [malli.generator :as mg]))

(def UserId :string)

(def Address
  [:map
   [:street :string]
   [:country [:enum "FI" "UA"]]])

;定义一个叫作User的schema
(def User
  [:map
   [:id #'UserId]
   [:address #'Address]
   [:friends [:set {:gen/max 2} [:ref #'User]]]])

; 使用这个schema随机值生成对象
(def user (mg/generate User))

; 使用这个schema验证生成的对象
(m/validate User user)

;定义schema的语法有三种：vector，map和lite
;默认的是vector语法，受hiccup启发：
;type
;[type & children]
;[type properties & children]

;类型
:string

;类型以及其属性
[:string {:min 1, :max 10}]

;类型，属性以及子项
[:tuple {:title "location"} :double :double]

;一个接收int类型并返回int类型的值的函数的schema
[:=> [:cat :int] :int]

;使用vector语法生成schema对象
(def non-empty-string
  (m/schema [:string {:min 1}]))

;检查schema是否合法
(m/schema? non-empty-string)
;true

(m/validate non-empty-string "")
;false

(m/validate non-empty-string "kikka")
;true

;返回schema对应的vector语法
(m/form non-empty-string)
;[:string {:min 1}]

;map语法，于内部使用，不要把它作为数据库持久化的模型

;类型
{:type :string}

;类型以及其属性
{:type       :string
 :properties {:min 1, :max 10}}

;类型，属性以及子项
{:type       :tuple
 :properties {:title "location"}
 :children   [{:type :double}
              {:type :double}]}

;一个接收int类型并返回int类型的值的函数的schema
{:type   :=>
 :input  {:type :cat, :children [{:type :int}]}
 :output {:type :int}
 }

;使用map语法生成schema对象
(def non-empty-str
  (m/from-ast {:type       :string
               :properties {:min 1}}))

(m/schema? non-empty-str)
;true

(m/validate non-empty-str "")
;false

(m/validate non-empty-str "kikka")
;true

;返回schema对应的map语法，也被称为schema ast
(m/ast non-empty-str)
;{:type :string, :properties {:min 1}}


;lite语法，从其namespace中就能看出其带有实验性质
(l/schema
  {:map1 {:x int?
          :y [:maybe string?]
          :z (l/maybe keyword?)}
   :map2 {:min-max     [:int {:min 0 :max 10}]
          :tuples      (l/vector (l/tuple int? string?))
          :optional    (l/optional (l/maybe :boolean))
          :set-of-maps (l/set {:e int?
                               :f string?})
          :map-of-int  (l/map-of int? {:s string?})}})

;option可以通过动态绑定*options*来使用
(binding [l/*options* {:registry (merge
                                   (m/default-schemas)
                                   {:user/id :int})}]
  (l/schema {:id    (l/maybe :user/id)
             :child {:id :user/id}}))

;为何要有多种语法？
;Malli最开始只有vector语法。它非常强大，相对容易阅读，但并不适用所有情况。
;之后引入了map语法，因为我们发现解析大量的vector语法的开销在运行于缓慢的单线程环境(如手机上的Javascript)时会成为一个问题。
;map语法允许惰性和无解析的模式创建。
;我们添加了lite语法，用于简化特殊情况下的schema创建，比如与reitit-coercion一起使用，以及从data-specs轻松迁移。
;实际上，vector和map语法中可以和schema对象混用，可以参考上面的User

(def Address
  [:map
   [:id string?]
   [:tags [:set keyword?]]
   [:address
    [:map
     [:street string?]
     [:city string?]
     [:zip int?]
     [:lonlat [:tuple double? double?]]]]])

;验证
;使用schema对象验证
(m/validate (m/schema :int) 1)
;true
;使用vector语法验证
(m/validate :int 1)
;true
(m/validate :int "1")
;false
(m/validate [:= 1] 1)
;true
(m/validate [:enum 1 2] 1)
;true
(m/validate [:and :int [:> 6]] 7)
;true
(m/validate [:qualified-keyword {:namespace :aaa}] :aaa/bbb)
;true
;生成验证函数，获得最佳的性能
(def valid?
  (m/validator
    [:map
     [:x :boolean]
     [:y {:optional true} :int]
     [:z :string]]))
(valid? {:x true, :z "kikka"})
;true

;schema可以有属性
(def Age
  [:and
   {:title               "Age"
    :description         "It's an age"
    :json-schema/example 20}
   :int [:> 18]])

(m/properties Age)
;{:title "Age", :description "It's an age", :json-schema/example 20}

;Map默认是开放的，意思是可以有没有在schema中声明的key
(m/validate
  [:map [:x :int]]
  {:x 1, :extra "key"})
; => true
;可以使用:close属性让Map不再开放
(m/validate
  [:map {:closed true} [:x :int]]
  {:x 1, :extra "key"})

;Map的spec中的key不限于keyword类型
(m/validate
  [:map
   ["status" [:enum "ok"]]
   [1 :any]
   [nil :any]
   [::a :string]]
  {"status" "ok"
   1        'number
   nil      :yay
   ::a      "properly awesome"})
;true

;最核心的predicate函数可以当成schema使用
(m/validate string? "kikka")
; => true

;枚举schema，注意，:enum后面的首个nil或map会被认为是它的属性
(m/validate [:enum 1 2 3] 3)
;true

;:map这种schema一般针对的是属性map（即有确定的key值的map）
;其中的key可以是对registry中已有的schema的引用，可以是带namespace的keyword或字符串
(m/validate
  [:map {:registry {::id      int?
                    ::country string?
                    "value"   number?}}
   ::id
   [:name string?]
   [::country {:optional true}]
   "value"]
  {::id    1
   :name   "kikka"
   "value" 123})

;可以使用:malli.core/default这个key来定义如何处理不在范围内的key
(m/validate
  [:map
   [:x :int]
   [:y :int]
   [::m/default [:map-of :int :int]]]
  {:x 1, :y 2, 1 1, 2 2})
;true
;它支持多重嵌套
(m/validate
  [:map
   [:x :int]
   [::m/default [:map
                 [:y :int]
                 [::m/default [:map-of :int :int]]]]]
  {:x 1, :y 2, 1 1, 2 2})
;true

;:seqable和:every这两个schema用于seqable?集合
;它们不同的地方在于对counted?和indexed?的集合的处理，以及它们对应的parser：
;1.:seqable解析其元素，但:every不解析并返回输入
;2.对parser的结果进行unparse，:seqable会丢失其原始的结合类型，而:every不会

;除此之外，:seqable会对集合的所有元素进行校验，所以它不能用于无穷集合
;而:every只校验:min,(inc :max)和(::m/coll-check-limit options 101)中的最大值的元素，
;当集合是counted?其是indexed?，:every也会检验整个集合
(m/validate [:seqable :int] #{1 2 3})
;true
(m/validate [:every :int] #{1 2 3})
;true
(m/validate [:seqable :int] (sorted-set 1 2 3))
;true
(m/validate [:seqable :int] (range 1000))
;true
(m/validate [:seqable :int] (conj (vec (range 1000)) nil))
;false
(m/validate [:every :int] #{1 2 3})
;true
(m/validate [:every :int] [1 2 3])
;true
(m/validate [:every :int] (sorted-set 1 2 3))
;true
(m/validate [:every :int] (vec (range 1000)))
;true
(m/validate [:every :int] (conj (vec (range 1000)) nil))
;false
;对于无穷的或无索引的集合，:every默认值check前1000个元素
(m/validate [:every :int] (concat (range 1000) [nil]))
;true
;可以显示设定要check的数量
(m/validate [:every {:max 2000} :int] (concat (range 1000) [nil]))
;false

;:sequential用于同质的sequential?集合
(m/validate [:sequential any?] (list "this" 'is :number 42))
;true
(m/validate [:sequential int?] [42 105])
;true
(m/validate [:sequential int?] #{42 105})
;false

;Malli也支持序列正则
;:cat和:catn用于串联
(m/validate [:cat string? int?] ["foo" 0])
;true
(m/validate [:catn [:s string?] [:n int?]] ["foo" 0])
;true
;:alt和:altn用于表示可选项
(m/validate [:alt keyword? string?] ["foo"])                ;=>
(m/validate [:altn [:kw keyword?] [:s string?]] ["foo"])    ; => true
;:?,:*,:+和:repeat用于重复，类似字符串的正则
(m/validate [:? int?] [])                                   ; => true
(m/validate [:? int?] [1])                                  ; => true
(m/validate [:? int?] [1 2])                                ; => false

(m/validate [:* int?] [])                                   ; => true
(m/validate [:* int?] [1 2 3])                              ; => true

(m/validate [:+ int?] [])                                   ; => false
(m/validate [:+ int?] [1])                                  ; => true
(m/validate [:+ int?] [1 2 3])                              ; => true

(m/validate [:repeat {:min 2, :max 4} int?] [1])            ; => false
(m/validate [:repeat {:min 2, :max 4} int?] [1 2])          ; => true
(m/validate [:repeat {:min 2, :max 4} int?] [1 2 3 4])      ; => true (:max is inclusive, as elsewhere in Malli)
(m/validate [:repeat {:min 2, :max 4} int?] [1 2 3 4 5])    ; => false
;其中，:catn和:altn允许命名子序列和可选项
(m/explain
  [:* [:catn [:prop string?] [:val [:altn [:s string?] [:b boolean?]]]]]
  ["-server" "foo" "-verbose" 11 "-user" "joe"])
;{:schema [:* [:catn [:prop string?] [:val [:altn [:s string?] [:b boolean?]]]]],
; :value ["-server" "foo" "-verbose" 11 "-user" "joe"],
; :errors ({:path [0 :val :s], :in [3], :schema string?, :value 11}
;          {:path [0 :val :b], :in [3], :schema boolean?, :value 11})}
;而:cat和:alt只能使用数字索引作为路径
(m/explain
  [:* [:cat string? [:alt string? boolean?]]]
  ["-server" "foo" "-verbose" 11 "-user" "joe"])
;{:schema [:* [:cat string? [:alt string? boolean?]]],
; :value ["-server" "foo" "-verbose" 11 "-user" "joe"],
; :errors ({:path [0 1 0], :in [3], :schema string?, :value 11}
;          {:path [0 1 1], :in [3], :schema boolean?, :value 11})}

;上面的例子都是序列正则接收非序列正则的子schema，如果想要实现序列正则的嵌套，需要使用:schema
(m/validate
  [:cat [:= :names] [:schema [:* string?]] [:= :nums] [:schema [:* number?]]]
  [:names ["a" "b"] :nums [1 2 3]])
;true

;针对不确定key且key-value类型一致的map，可以使用:map-of
(m/validate
  [:map-of :string [:map [:lat number?] [:long number?]]]
  {"oslo"     {:lat 60 :long 11}
   "helsinki" {:lat 60 :long 24}})
;; => true

(defmacro definstrumented
  [name schema lambda-list & body]
  `(def ~(with-meta name schema)
     (m/-instrument
       ~schema
       (fn ~lambda-list ~@body))))

(definstrumented hehe
                 {:schema [:=> [:cat :int :int] [:int {:max 6}]] :report println}
                 [a b]
                 (* a b))
(hehe 1 2)
