(ns awesome.byte-streams.examples
  (:require [byte-streams :as bs]
            [byte-streams.graph :as g]
            [manifold.stream :as s])
  (:import (java.nio ByteBuffer)
           (java.io File)
           (java.nio.channels WritableByteChannel)))

;byte-streams是为了解决Java中不同的表示byte流的类型之间转换问题

;convert可以把一个对象转换为另一个类型的对象
(def buf (bs/convert "abcd" ByteBuffer))
;#object[java.nio.HeapByteBuffer 0x27a32acc "java.nio.HeapByteBuffer[pos=0 lim=4 cap=4]"]
(bs/convert buf String)
;"abcd"

;conversion-path可以查找两个类型之间的转换路径
(bs/conversion-path String ByteBuffer)
;([java.lang.String [B] [[B java.nio.ByteBuffer])

;对于常用的转换，有不少的便利方法可以使用
(bs/to-byte-array "abc")
(bs/to-byte-buffer "abc")
;可以把一个字符串seq转换为输入流，并转为ByteBuffer的seq
(take 2
      (bs/convert
        (bs/to-input-stream (take 1000 (repeat "hello")))
        (bs/seq-of ByteBuffer)                              ;seq-o返回一个自定义的Type对象表示某个类的seq类型
        {:chunk-size 128}))

;转换为一个manifold的stream
(def stream (bs/convert ["a" "b" "c"] (bs/stream-of String)))
@(s/take! stream)
;"a"

;可以自定义转换器
;(bs/def-conversion [String File]
;                   [s options]
;                   (let [file (io/as-file (:file options))]
;                     (with-open [writer (io/writer file)]
;                       (.write writer s 0 (count s))
;                       (.flush writer))
;                     file))
;(bs/convert "abc" File {:file "test.txt"})

;可以使用transfer在任意可以产生字节的和任意可以接受字节的对象之间建立管道进行传输
(def f (File. "test.txt"))
(bs/transfer "abc" f {:append? true})
;目前:append?在mac上不起作用，用下面的代码却没有问题
(.write (bs/convert f WritableByteChannel) (bs/convert "abc" ByteBuffer))

;可以使用def-transfer自定义传输器

(bs/print-bytes (-> #'bs/print-bytes meta :doc))
;50 72 69 6E 74 73 20 6F  75 74 20 74 68 65 20 62      Prints out the b
;79 74 65 73 20 69 6E 20  62 6F 74 68 20 68 65 78      ytes in both hex
;20 61 6E 64 20 41 53 43  49 49 20 72 65 70 72 65       and ASCII repre
;73 65 6E 74 61 74 69 6F  6E 73 2C 20 31 36 20 62      sentations, 16 b
;79 74 65 73 20 70 65 72  20 6C 69 6E 65 2E            ytes per line.

;对字符进行比较
(bs/compare-bytes "abc" "abd")
;-1
;检查两个字符容器中的字符是否相等
(bs/bytes= "abc" "abc")
;true

;列出一个类的所有可能的转换路径
(bs/possible-conversions String)

;是否有优化的传输器
(bs/optimized-transfer? (g/type String) (g/type File))
;true
