(ns com.github.zjjfly.awesome.transit.examples
  (:require [cognitect.transit :as transit])
  (:import (java.io ByteArrayOutputStream ByteArrayInputStream)))

(def out (ByteArrayOutputStream. 4096))

(def writer (transit/writer out :json))
(transit/write writer "foo")
(transit/write writer {:a [1 2]})
(.toString out)
;"[\"~#'\",\"foo\"] [\"^ \",\"~:a\",[1,2]]"

(def in (ByteArrayInputStream. (.toByteArray out)))
(def reader (transit/reader in :json))
(prn (transit/read reader))
;"foo"
(prn (transit/read reader))
;{:a [1 2]}
