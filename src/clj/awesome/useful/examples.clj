(ns awesome.useful.examples
  (:require [flatland.useful.macro :as macro]
            [flatland.useful.bean :as bean]
            [flatland.useful.datatypes :as dt]
            [flatland.useful.debug :as debug]
            )
  (:import (com.github.zjjfly.awesome User)))

;匿名macro
(macro/anon-macro
  [x y]
  `(def ~x ~y) myconst 10)
myconst
;10
(macro/anon-macro
  [x & args]
  `(def ~x [~@args]) v 1 2 3)
v
;[1 2 3]
;定义一个匿名宏,并执行这个宏,根据参数的数量来决定执行的次数
(macro/macro-do [f & args]
                `(def ~(symbol (str "basic-" f))
                   (partial ~f ~@args))
                + 1 2 3)
(basic-+ 4)
;10

;bean
;property-setters获取类的所有属性的setter
(:age (bean/property-setters User))
;update-bean批量设置对象的某些属性的值
(def user (User. "jjzi" 18))
(bean/update-bean user {:name "zkx" :age (int 1)})
(.getName user)
;"zkx"
(.getAge user)
;1

;data types
(dt/as-int "12")
;12
(dt/as-int 1.2)
;1
(defrecord Person [name age])
;make-record可以更加灵活的通过map生成record,而不需要和map->Person这样具体的方法绑定
(def record (dt/make-record Person {:name "jjzi" :age 2}))
record
;#awesome.useful.examples.Person{:name "jjzi", :age 2}
;assoc-record,需要在record之前加类型提示
(dt/assoc-record ^Person record {:age 1})
;;#awesome.useful.examples.Person{:name "jjzi", :age 2}
;update-record更新record中的字段,后面的参数是若干个form,form中如果有record的字段的关键字
;会在宏中改成record中该字段的值
(dt/update-record ^Person record (inc :age))
; #awesome.useful.examples.Person{:name "jjzi", :age 2}
(macroexpand '(dt/record-accessors User))
(:age record)
;2

;debug
;?可以包裹一个form,它会在控制台打印这个form和它的结果,并返回结果
(debug/? (+ 1 1))
;?!和?基本相同,但不是打印到控制台,而是一个文件,默认是临时文件,用户也可以指定文件
(debug/?! (+ 1 1))
(slurp "/tmp/spit")
(debug/?! "./my.log" (+ 1 1))
(slurp "./my.log")
