(ns com.github.zjjfly.awesome.gloss.examples
  (:require [gloss.core :as gloss]
            [gloss.io :as io]
            [byte-streams :as bs]))

;gloss是一个定义协议栈,并可以方便的为其生成编解码器的库,文档地址:https://aleph.io/codox/gloss/gloss.core.html

;bit-map定义一个顺序的map的frame,它可以对clojure的map进行编码和解码
;参数是任意多个keyword和长度的键值对,长度指的是有符号数字所占的位长
;所有的keyword的长度和必须是8的倍数,否则会报错.这个和除以8就是这个frame占多少个byte
;如果某个keyword长度为1,那么会解码为true和false
(def f1 (gloss/bit-map :a 3 :b 1 :c 4))

;bit-seq定义一个由特定位长的无符号整数的序列组成的frame.
;位长的和必须是8的倍数,否则会报错.这个和除以8就是这个frame占多少个byte
;如果一个数字大于它的位长所能表示的最大值,那么它会在编码的时候被截取
(def f2 (gloss/bit-seq 7 4 5))

;byte-count,返回任意可以转成ByteBuffers序列的对象中的byte的数量
(gloss/byte-count (io/encode f1 {:a 7 :b false :c 16}))

;compile-frame,编译frame,获得一个编解码器,可以传入一个编码预处理函数和解码后处理函数,这可以让frame变成byte数组和clojure数据类型之间的中间表示
(def codec (gloss/compile-frame f2 (fn [{:keys [a b c]}] [a b c]) (fn [[a b c]] {:a a :b b :c c})))
(io/decode codec (io/encode codec {:a 3 :b 1 :c 2}))
;{:a 3 :b 1 :c 2}

;defcodec,编译frame,并为其绑定一个标识.还有一个defcodec-用于定义命名空间私有的codec
(gloss/defcodec c f1)
(io/decode c (io/encode c {:a 3 :b 1 :c 2}))
;{:a 3, :b true, :c 2}

;enum,接收一个枚举列表或枚举到数字的映射,返回一个为每一个枚举关联一个唯一值的codec
;如果传入的列表,这个唯一值默认是从0开始,和Java的枚举类似
(gloss/defcodec enum-codec (gloss/enum :byte :a :b :c))
(def encoded-enum (io/encode enum-codec :b))
(gloss/byte-count encoded-enum)
;1
(.get (first encoded-enum))
;1

;finite-block,定义一个byte序列,有固定长度或者有一个数字前缀来表示它的长度
(gloss/defcodec finite-codec1 (gloss/finite-block 3))
(def buffer1 (io/to-byte-buffer [1 2 3]))
(io/decode finite-codec1 (io/encode finite-codec1 buffer1))
;(#object[java.nio.HeapByteBuffer 0x5491f6d3 "java.nio.HeapByteBuffer[pos=0 lim=3 cap=3]"])
(gloss/defcodec finite-codec2 (gloss/finite-block (gloss/prefix :int32)))
(def buffer2 (io/to-byte-buffer [0 0 0 4 1 2 3 4]))
(.getInt buffer2)
(io/encode finite-codec2 (io/encode finite-codec2 buffer2))

;finite-frame,可以为一个现有的frame指定一个固定的长度或或一个长度前缀
(def ff (gloss/finite-frame 2 f2))
(io/decode ff (io/encode ff [2 1 3]))
;(2 1 3)

;string用于定义一个字符串帧,可以指定这个帧的编码格式,终结符,长度
(def sc (gloss/string :utf-8 :delimiters ["\r\n" "\r"]))
(io/decode-all sc (io/encode-all sc ["123" "345"]))
;["123" "345"]
