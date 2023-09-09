(ns awesome.aero.examples
  (:require [aero.core :as aero])
  (:import (java.io ByteArrayInputStream)))

;aero是一个小的库,用于处理显式的配置
;它的设计目标:
;1.配置需要是明显的,明确的
;2.避免重复的配置,但允许差异
;3.允许配置放在源码仓库中
;4.可以隐藏一些隐私配置
;5.配置必须是数据,而不是一些智能的配置程序
;6.尽量少用环境变量
;7.使用edn格式的配置,这是clojure社区的标准

;读取配置
;(aero/read-config "config.edn")
;{:greeting "Hello,World"}

;上面的做法在代码位于jar中的时候,会出错,所以尽量在classpath中读取
(aero/read-config (clojure.java.io/resource "config.edn"))
;{:greeting "Hello,World"}

(defmacro read-str-config [s & m]
  `(aero/read-config (ByteArrayInputStream. (into-array Byte/TYPE ~s)) ~@m))

(defmacro read-cp-config [f & m]
  `(aero/read-config (clojure.java.io/resource ~f) ~@m))

;aero提供了一个标签字面量库,用于增强配置能力
;#env 表示这个值是环境变量
(read-str-config "{:user #env USER}")
;{:user "zjjfly"}

;#envf 把环境变量的值插入一个格式化字符串
(read-str-config "{:who #envf [\"This is %s.\" USER]}")
;{:who "This is zjjfly."}

;#or 可以提供多个选项,最后一个选项是默认值
(read-str-config "{:port #or [#env PORT 8080]}")
;{:port 8080}

;#join 可以拼接多个值,多用于构建连接字符串
(read-str-config "{:url #join [\"jdbc:postgresql://psq-prod/prod?user=\"
                                #env USER
                                \"&password=\"
                                #or [#env PROD_PASSWD 12345]]}")
;{:url "jdbc:postgresql://psq-prod/prod?user=zjjfly&password=12345"}

;#profile,类似maven的profile
(read-str-config "{:webserver
 {:port #profile {:default 8000
                  :dev 8001
                  :test 8002}}}" {:profile :dev})
;{:webserver {:port 8001}}

;#hostname 根据环境变量HOST_NAME提供对应的配置值
(read-str-config "{:webserver
                    {:port #hostname {\"stone\" 8080
                                       #{\"emerald\" \"diamond\"} 8081
                                       :default 8082}}}")
;{:webserver {:port 8082}}

;#user 类似#hostname,但根据的是环境变量USER
(read-str-config "{:webserver
                    {:port #user {\"zjjfly\" 8080
                                       #{\"emerald\" \"diamond\"} 8081
                                       :default 8082}}}")
;{:webserver {:port 8080}}

;#long,#double,#keyword,#boolean 可以把配置的值转成别的类型(默认是字符串)
(read-str-config "{:debug #boolean #or [#env DEBUG \"true\"]
                    :webserver
                      {:port #long #or [#env PORT 8080]
                         :factor #double #or [#env FACTOR 1.0]
                            :mode #keyword #env MODE}}")
;{:debug true, :webserver {:port 8080, :factor 1.0, :mode :}}

;#include 引入另一个配置文件,这可以避免配置文件过大.
;include引入的文件的路径是相对路径,相对的是根配置文件
(read-cp-config "composed.edn")
;{:webserver {:host "1.1.2.2", :port 8080}, :analytics {:factor 2.0, :mode :simple}}

;可以使用自定义的解析器或aero提供的别的解析器来改变寻找引用的配置文件的方式
;aero/resource-resolver是直接从classpath中查找
(read-cp-config "composed.edn" {:resolver aero/resource-resolver})
;如果是一个map,则其中的key是#include中配置的文件名,value是实际的文件的路径
(read-cp-config "composed.edn"
                {:resolver {"webserver.edn" "src/resources/webserver.edn"
                            "analytics.edn" "src/resources/analytics.edn"}})
;{:webserver {:host "1.1.2.2", :port 8080}, :analytics {:factor 2.0, :mode :simple}}

;#merge 合并多个map
(read-str-config "#merge [{:foo bar} {:bar foo}]")
;{:foo bar, :bar foo}

;#ref 引用相同配置中的其他部分,后面跟一个vector,内部它会被传入get-in去定位被引用的配置.
;#ref可以在include的文件中使用
(read-str-config "{:db-connection \"datomic:dynamo://dynamodb\"
                    :webserver
                      {:db #ref [:db-connection]}
                       :analytics
                         {:db #ref [:db-connection]}}")
;{:db-connection "datomic:dynamo://dynamodb",
; :webserver {:db "datomic:dynamo://dynamodb"},
; :analytics {:db "datomic:dynamo://dynamodb"}}

;可以自定义tag,只要扩展多重方法aero/reader
(defmethod aero/reader 'mytag
  [{:keys [profile] :as opts} tag value]
  (if (= value :favorite)
    :chocolate
    :vanilla))
(read-str-config "#mytag :favorite}")
;:chocolate
(read-str-config "#mytag :foo}")
;:vanilla

