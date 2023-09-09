(ns awesome.aleph.examples
  (:require [aleph.http :as http]
            [manifold.deferred :as d]
            [manifold.stream :as s]
            [byte-streams :as bs]))

;aleph可以把从网络中获取的数据包装为Manifold的stream的工具包
;aleph目前提供了对HTTP，TCP和UDP的包装器

;aleph遵守Ring的规范，所以可以作为一个HTTP服务器
;handle可以返回一个Manifold的deferred，或者返回的body是一个stream
(defn handler [req]
  {:status 200
   :headers {"content-type" "text/plain"}
   :body (->> [1 4 3]
              (s/map inc)
              s/stream->seq)})

;(http/start-server #'handler {:port 8080})

;aleph也可以作为HTTP客户端，底层是使用的clj-http
(-> @(http/get "https://www.baidu.com/")
    :body
    bs/to-string
    prn)
(d/chain (http/get "https://www.baidu.com")
         :body
         bs/to-string
         prn)
