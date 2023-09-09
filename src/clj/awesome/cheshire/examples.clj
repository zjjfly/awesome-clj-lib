(ns awesome.cheshire.examples
  (:require [cheshire.core :as cheshire]
            [cheshire.generate :as cgenerate])
  (:import (java.awt Color)
           (java.net URL)))

;cheshire是json的编码解码器.相比clojure-json和clj-json,它比前者的性能高,比后者的特性更多
;它支持更多的类型,并且可以自定义编码器

;json字符串转为map
(cheshire/parse-string "{\"foo\":\"bar\"}")
;{"foo" "bar"}

;json字符串转为map,json中的key转为clojure的keyword
(cheshire/parse-string "{\"foo\":\"bar\"}" true)
;{:foo "bar"}

;json字符串转为map,使用自定义函数对json的key做转换
(cheshire/parse-string "{\"foo\":\"bar\"}" (fn [k] (keyword (.toUpperCase k))))
;{:FOO "bar"}

;在cheshire中,一个字符串也是一个合法的json
(cheshire/parse-string "\"foo\"")
;"foo"

;读取一个stream中的json字符串,如果有多个json,只会返回第一个的解析结果
(cheshire/parse-stream (clojure.java.io/reader "./my.log"))
;{"timestamp" "2020-07-12T04:59:39Z", "level" "info", "event" "as"}
;读取一个stream中的json字符串lazily,返回的是一个seq,可以支持多个json
(cheshire/parsed-seq (clojure.java.io/reader "./my.log"))
;({"timestamp" "2020-07-12T04:59:39Z", "level" "info", "event" "as"}
; {"timestamp" "2020-07-12T06:05:21Z", "level" "error", "event" ""})

;2.0.4已经更高版本,可以使用一个函数来指定特定的字段的解析后返回的类型
(cheshire/parse-string "{\"myarray\":[2,3,3,2],\"myset\":[1,2,2,1]}" true
        (fn [field-name]
          (if (= field-name "myset")
            #{}
            [])))
;{:myarray [2 3 3 2], :myset #{1 2}}

;自定义编码器
(cgenerate/add-encoder Color
                       (fn [c jsonGenerator]
                         (.writeString jsonGenerator (str c))))
(cheshire/generate-string (Color. 1 2 3))
;"\"java.awt.Color[r=1,g=2,b=3]\""

;一个内置的编码器,它的行为是对要编码的对象调用str方法
(cgenerate/add-encoder URL cgenerate/encode-str)
(cheshire/generate-string (URL. "http://www.google.com"))
;"\"http://www.google.com\""

;删除编码器
(cgenerate/remove-encoder Color)
;nil
(try (cheshire/generate-string (Color. 1 2 3))
     (catch Exception e
       (assert (not= e nil))))

;指定生成json字符串的时候对key的处理函数
(cheshire/generate-string {:a 1 :b 2})
