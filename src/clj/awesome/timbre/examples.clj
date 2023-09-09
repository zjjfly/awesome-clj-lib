(ns awesome.timbre.examples
  (:require
    [taoensso.timbre :as timbre
     :refer [log trace debug info warn error fatal report
             logf tracef debugf infof warnf errorf fatalf reportf
             spy get-env]]
    [taoensso.timbre.appenders.core :as core-appenders]
    [cheshire.core :as cheshire]))

;timbre是一个clojure的日志框架,它简单,高性能,并且支持自定义appender middleware来支持多种输出源
;现有的第三方middleware有redis,email,logcat,udp socket,slack,postgresql等

;打印INFO日志
(info "This will print out")
;xxx INFO [awesome.timbre.examples:7] - This will print out
;spy打印日志并附带原始的表达式
(spy :info (* 5 4 3 2 1))
;xxx INFO [awesome.timbre.examples:12] - (* 5 4 3 2 1) => 120
;使用get-env获取当前的局部变量
(defn my-mult [x y]
  (let [z (+ x y)]
   (info "Lexical env:" (get-env)) (* x y z)))
(my-mult 1 2)
;xxx INFO [awesome.timbre.examples:1] - Lexical env: {x 1, y 2}

;打印TRACE日志,默认不会输出,因为默认的日志等级比trace高
(trace "This won't print due to insufficient log level")
;nil
;timbre的函数对于异常的打印对程序员更友好
(info (Exception. "Oh noes") "arg1" "arg2")

;使用log-errors打印错误
(timbre/log-errors
  (/  1 0))
;nil

;使用log-and-rethrow-errors通用可以打印错误信息,但会把这个异常抛出
(try
  (timbre/log-and-rethrow-errors
  (/  1 0))
  (catch Exception e
         (assert (not= e nil))))

;类似log-errors,但会立即返回一个future
(timbre/logged-future (/  1 0))

;配置timbre,timbre的配置就是一个简单的map
;set-config!使用新的配置替换旧的
(timbre/set-config! timbre/example-config) ;example-config是一个实例配置
;merge-config! 把旧的配置和新的合并
(timbre/merge-config! {:level :trace})
(trace "This will print")

;自定义timbre日志格式,决定日志格式的是一个函数,签名是fn [data] => string
;data是一个存放日志和\相关信息的map,包括日志的实际消息,时间戳,主机名,日志等级...
(defn- json-output [{:keys [level msg_ instant]}]
  (let [msg (force msg_)
        event (if (empty? msg)
                ""
                (read-string msg))] ;timbre的日志消息体是delay的,所以要用force求值
    (cheshire/generate-string {:timestamp instant
                               :level level
                               :event event})))
(timbre/merge-config! {:output-fn json-output})
(info "hello")
;{"timestamp":"2020-07-12T01:45:34Z","level":"info","event":"hello"}

;内置的appender,可以把日志写到文件中
(timbre/merge-config!
  {:appenders {:spit (core-appenders/spit-appender {:fname "./my.log"})}})
(info "hello")
(delay)
