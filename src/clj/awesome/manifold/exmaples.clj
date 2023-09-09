(ns awesome.manifold.exmaples
  (:require [manifold
             [deferred :as d]
             [bus :as bus]
             [stream :as s]
             [executor :as e]
             ]
            [clojure.java.io :as io]))

;manifold提供了一套可以把不同的表示还未获得的数据（如Java的Future，RxJava等）统一起来的API，
;并包括了对其操作的工具

;定义一个还未获得的值
(def dd (d/deferred))

;放入一个值
(d/success! dd :foo)

;对其解引用
@dd
;:foo

;和Clojure的future一样，解引用出来的也可以是异常
(def dd (d/deferred))
(d/error! dd (Exception. "Boom!"))
;@d

;可以注册回调函数来处理正常情况和异常情况的值
(def dd (d/deferred))
(d/on-realized dd
               #(println "success!" %)
               #(println "error!" %)
               )
(d/success! dd :foo)
;success! :foo

;应该避免使用on-realized，而是使用chain，它类似->>
(def dd (d/deferred))
(d/chain dd inc inc inc #(println "x + 3 = " %))
(d/success! dd 0)
;x + 3 =  3

;如果chain串起来的某个函数在执行的时候抛出了异常，那么后面的函数都不会执行，chain返回的deferred也会产生一样的异常
;为了处理这种情况，需要使用catch方法
(def dd (d/deferred))
(-> dd
    (d/chain dec #(/ 1 %))
    (d/catch Exception #(println "whoops, that didn't work:" %)))

(d/success! dd 1)
;whoops, that didn't work: #error {
; :cause Divide by zero
; ...}

;zip可以把多个deferred组合成单个deferred
(d/zip (future 1) (future 2) (future 3))
;<< (1 2 3) >>

;timeout!可以指定deferred的超时时间，如果在指定的时间内没有产生值，则会抛出异常或一个默认值
@(d/timeout!
   (d/future (Thread/sleep 1000) :foo)
   100
   :bar)
;:bar

;clojure的promise和future可以看成是deferred，但对它们的解引用操作是阻塞的，Manifold为了使其变为异步的必须要分配线程
;所以，尽可能地使用manifold.deferred/deferred代替promise，manifold.deferred/future代替future

;使用let-flow可以声明不同的deferred，并在后续的form值直接把它们当成实际的值使用
;let-flow返回的也是一个deferred
(defn some-service [time n]
  (d/future (Thread/sleep time) n))

(defn deferred-sum []
  (d/let-flow [a (some-service 1000 1)
               b (some-service 1100 2)]
              (+ a b)))
(time @(deferred-sum))
;"Elapsed time: 1105.615833 msecs"
;=> 3

;manifold.deferred/loop，manifold.deferred/recur和clojure标准库值的loop,recur类似
;它允许异步的循环，返回的也是一个deferred
(defn file-line-consume [file func]
  (d/loop [reader (io/make-reader file {:encoding "UTF-8"})]
          (d/chain (.readLine reader)
                   #(doto %
                      func)
                   #(when-not (nil? %)
                      (d/recur reader)))))

(file-line-consume "deps.edn" println)

;manifold stream
;stream方法可以初始化一个stream，可以指定一个缓冲区的大小
(def ss (s/stream 10))

;stream既是生产者，也是消费者。put!方法把消息放入stream，take!可以从stream拿消息
;这两个方法的返回都是一个deferred
(def p (s/put! ss 1))
@p
;put!返回的deferred中的值是布尔值，表示是否成功把值放入stream
(def t (s/take! ss))
;<< 1 >>
@t
;1
;take!返回的deferred中的是取出的信息

;close!可以关闭stream的生产者
(s/close! ss)
;stream在close之后，再调研put!会返回false
(def p (s/put! ss 1))
@p
;false

;使用close?可以判断stream是否关闭了
(s/closed? ss)
;true
;使用on-closed可以注册一个stream关闭的回调函数
(s/on-closed ss #(println "stream closed!"))

;一个stream不会再生产消息的状态（一般是因为被关闭了）被称为drained
;drained?可以判断一个stream是否drained，on-drained可以注册一个drained的回调函数
(s/drained? ss)
;true
(s/on-drained ss #(println "stream drained!"))
;一般情况下，对drained的stream调用take!返回nil，但如果nil就是消息本身，那么需要再指定一个表示drained的值
@(s/take! ss :drained)
;:drained

;可以使用try-put!和try-take!指定put和take的超时时间以及超时的时候的返回的默认值
(def ss (s/stream))
@(s/try-put! ss :foo 1000 :timeout)
;:timeout
@(s/try-take! ss :drained 1000 :timeout)
;:timeout

;最简单的对stream的操作是消费它生产的每一条消息
(s/consume #(prn 'message! %) ss)
@(s/put! ss :foo)
;true

;manifold还提供了一些和clojure的seq操作类似的函数
(->> [1 2 3]
     s/->source
     (s/map inc)
     s/stream->seq)
;(2 3 4)
;map如果操作的是clojure的seq，那么它会自动的调用->source转成stream
(->> [1 2 3]
     (s/map inc)
     s/stream->seq)
;(2 3 4)

;因为stream不是immutable的，所以如果要把它当成seq对待，需要调用stream->seq
(->> [1 2 3]
     s/->source
     s/stream->seq
     (map inc))
;(2 3 4)

;可以从源stream派生出多个stream，源stream中的消息也会传递到所以派生出的stream
(def ss (s/stream))
(def a (s/map inc ss))
(def b (s/map dec ss))
@(s/put! ss 0)
;true
@(s/take! a)
;1
@(s/take! b)
;-1
;如果源stream关闭了，它的所有派生stream也会关闭。反之，如果所有的派生stream关闭了，那么源stream也会关闭
(s/close! ss)
(s/closed? ss)
;true
(s/drained? a)
;true
(s/drained? b)
;false

;对于任何的在manifold值没有等价物的clojure函数，可以使用manifold.stream/transform,它接受一个transducer
(->> [1 2 3]
     (s/transform (map inc))
     s/stream->seq)
;(2 3 4)

;periodically函数类似repeatedly，但它会生成一个每隔指定的毫秒放入函数的的执行结果的stream
(def ps (s/periodically 1000 #(rand-int 10)))
@(s/take! ps)

;connect函数可以把两个stream连起来，这样前一个stream的所有消息会进入到后一个stream
(def a (s/stream))
(def b (s/stream))
(s/connect a b
           {:description "a connection"                     ;connect的参数
            :downstream? true
            :upstream?   true
            :timeout     1000}
           )
@(s/put! a 1)
;true
@(s/take! b)
;1

;description获取stream的描述信息
(s/description a)
;{:pending-puts 0,
; :drained? false,
; :buffer-size 0,
; :permanent? false,
; :type "manifold",
; :sink? true,
; :closed? false,
; :pending-takes 1,
; :buffer-capacity 0,
; :source? true}

;downstream可以查找一个stream的downstream,它返回一个两个元素的tuple，
;第一个是stream本身的描述，第二个它的downstream的描述信息
(s/downstream a)
;(["a connection"
;  <<
;  stream:
;  {:pending-puts 0,
;   :drained? false,
;   :buffer-size 0,
;   :permanent? false,
;   :type "manifold",
;   :sink? true,
;   :closed? false,
;   :pending-takes 0,
;   :buffer-capacity 0,
;   :source? true}
;  >>])

;connect-via可以在消息从源stream到downstream之前使用一个callback进行处理，
;这个callback返回一个布尔类型的deferred，如果返回false，表示downstream关闭了
;它是map和filter等函数的实现基础。connect-via提供了backpressure，
;当callback返回的deferred实际产生值的时候，源stream才能放入新的消息
(def a (s/stream))
(def b (s/stream 10))
(s/connect-via a #(s/put! b (inc %)) b)
@(s/put! a 1)
;true
@(s/take! b)
;2

(def a (s/stream))
(s/map inc a)
(s/downstream a)
;([{:op "map"} << sink: {:type "callback"} >>])

;可以使用throttle限制stream的速率
;如限制每秒1000条数据：
(s/throttle 1000 a)

;manifold.bus提供了简单的发布/订阅的实现
;初始化一个event bus，可以指定订阅的时候生成的stream的buffer的大小
(def b (bus/event-bus #(s/stream 100)))
;订阅返回的是一个stream
(def sub (bus/subscribe b :topicA))
;publish!返回一个deferred，只有当所有消息都被订阅者收到之后，才会realize
(bus/publish! b :topicA "msg")
@(s/take! sub)
;"msg"

;可以指定deferred和stream的线程池，一般不会用到
(def executor (e/fixed-thread-executor 42))
(-> (d/future 1)
    (d/onto executor)
    (d/chain inc inc inc))

(->> (s/->source (range 1e3))
     (s/onto executor)
     (s/map inc))
