(ns awesome.potemkin.examples
  (:require [potemkin]))

;import-vars可以把其他ns中的var复制到当前的ns中,即直接在当前ns中定义一个一模一样的var
(potemkin/import-vars
  [clojure.walk prewalk postwalk]
  [clojure.string join])

(join "," (range 1 10))
;"1,2,3,4,5,6,7,8,9"

(postwalk
  (fn [x]
    (let [new (if (number? x) (* x 2) x)]
      (printf "%s -> %s\n" x new)
      new))
  '(1 (2 3)))
;1 -> 2
;2 -> 4
;3 -> 6
;(4 6) -> (4 6)
;(2 (4 6)) -> (2 (4 6))
;=> (2 (4 6))


;下面的代码会报错,原因是clojure.data这个ns不在(all-ns)中
;(potemkin/import-vars [clojure.data diff])


;定义自己的clojure的map要实现很多接口,def-map-type可以简化这个过程
(potemkin/def-map-type LazyMap [m mta]
                       (get [_ k default-value]
                            (if (contains? m k)
                              (let [v (get m k)]
                                (if (instance? clojure.lang.Delay v)
                                  @v
                                  v))
                              default-value))
                       (assoc [_ k v]
                         (LazyMap. (assoc m k v) mta))
                       (dissoc [_ k]
                               (LazyMap. (dissoc m k) mta))
                       (keys [_]
                             (keys m))
                       (meta [_]
                             mta)
                       (with-meta [_ mta]
                                  (LazyMap. m mta)))
(def m (LazyMap. {:a 1 :b 2} {:private true}))
(assoc m :c 3)
;{:a 1, :b 2, :c 3}
(empty m)
;{}
(meta m)
;{:private true}

;def-derived-map类似于def-map-type,它可以通过构造参数加入一些默认的键值对
(potemkin/def-derived-map StringProperties [^String s]
                          :base s
                          :lower-case (.toLowerCase s)
                          :upper-case (.toUpperCase s))
(->StringProperties "abc")
;{:lower-case "abc", :upper-case "ABC", :base "abc"}

;memorize的高性能版本,比原生的快得多
(potemkin/fast-memoize inc)

;如果语法quote中嵌套了语法quote,使用符号前缀加#号的自动生成符号的做法会产生不一致的结果,使用unify-gensyms可以避免
(potemkin/unify-gensyms
  `(let [x## 1]
     ~@(map
         (fn [n] `(+ x## ~n))
         (range 3))))
