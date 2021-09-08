(ns com.github.zjjfly.awesome.core.async.examples
  (:require [clojure.core.async :as async]))

; 同步发送和接收消息
(let [c (async/chan 10)]
  (async/>!! c "hello")
  (assert (= "hello" (async/<!! c)))
  (async/close! c))

;使用thread方法,传入一个supplier,它会在另一个线程中执行,并将它的结果放到一个channel中,最后返回这个channel
(->
  (async/thread "hello")
  async/<!!
  (= "hello"))

;使用go,其中的阻塞的channel操作只会暂停执行,而不会阻塞线程
(let [c (async/chan)]
  (async/go (async/>! c "hello"))
  (assert (= "hello" (async/<!! (async/go (async/<! c)))))
  (async/close! c))

;alts!!类似go的select,可以同时等待多个channel的输出
(let [c1 (async/chan)
      c2 (async/chan)]
  (async/thread (while true
                  (let [[v ch] (async/alts!! [c1 c2])]
                    (println "Read" v "from" ch))))
  (async/>!! c1 "hi")
  (async/>!! c2 "there"))

;alts!是在go中使用的alts!!
(let [c1 (async/chan)
      c2 (async/chan)]
  (async/go (while true
              (let [[v ch] (async/alts! [c1 c2])]
                (println "Read" v "from" ch))))
  (async/go (async/>! c1 "hi"))
  (async/go (async/>! c2 "there")))

(let [n 1000
      cs (repeatedly n async/chan)
      begin (System/currentTimeMillis)]
  (doseq [c cs] (async/go (async/>! c "hi")))
  (dotimes [i n]
    (let [[v c] (async/alts!! cs)]
      (assert (= "hi" v))))
  (println "Read" n "msgs in" (- (System/currentTimeMillis) begin) "ms"))

(let [c (async/chan)]
  (async/go (do
              (Thread/sleep 3000)
              (async/>! c "hello")))
  (async/<!! (async/go
               (async/<! c)))
  (async/close! c))

;实现类似go的time.After
(defn after [f ms]
  (async/go
    (async/<! (async/timeout ms))
    (f)))

(defn receive-n-maps
  "Receive n items from the given channel and return them as a vector."
  [c n]
  (loop [i 0
         res []]
    (if (= i n)
      res
      (recur (inc i) (conj res (async/<!! c))))))

(defn receive-n
  "Receive n items from the given channel and return them as a vector."
  [c n]
  (loop [i 0
         res []]
    (if (= i n)
      res
      (recur (inc i) (conj res (async/<!! c))))))

(defn factorize
  "Naive factorization function; takes an integer n and returns a vector of
  factors."
  [n]
  (if (< n 2)
    []
    (loop [factors []
           n n
           p 2]
      (cond (= n 1) factors
            (= 0 (mod n p)) (recur (conj factors p) (quot n p) p)
            (>= (* p p) n) (conj factors n)
            (> p 2) (recur factors n (+ p 2))
            :else (recur factors n (+ p 1))))))

(defn async-go-factorizer
  "Parallel factorizer for nums, launching n go blocks."
  [nums n]
  ;;; Push nums into an input channel; spin up n go-blocks to read from this
  ;;; channel and add numbers to an output channel.
  (let [in-c (async/chan)
        out-c (async/chan)
        ;f (memoize factorize)
        f factorize
        ]
    (async/onto-chan!! in-c nums)
    (dotimes [i n]
      (async/go-loop []
                     (when-let [nextnum (async/<! in-c)]
                       (async/>! out-c {nextnum (f nextnum)})
                       (recur))))
    (receive-n-maps out-c (count nums))))

(defn async-thread-factorizer
  "Same as async-go-factorizer, but with thread instead of go."
  [nums n]
  (let [in-c (async/chan)
        out-c (async/chan)
        f factorize
        ]
    (async/onto-chan!! in-c nums)
    (dotimes [i n]
      (async/thread
        (loop []
          (when-let [nextnum (async/<!! in-c)]
            (async/>!! out-c {nextnum (f nextnum)})
            (recur)))))
    (receive-n-maps out-c (count nums))))

(defn async-with-pipeline
  "Parallel factorizer using async/pipeline."
  [nums n]
  (let [in-c (async/chan)
        out-c (async/chan)]
    (async/onto-chan!! in-c nums)
    (async/pipeline n out-c (map #(hash-map % (factorize %))) in-c)
    (receive-n-maps out-c (count nums))))

;go在应付存在阻塞操作的时候,性能是没有thread好的,所以go最好是和非阻塞的api一起用
(defn launch-go-blocking-and-compute
  [nblock ncompute]
  (let [c (async/chan)]
    (dotimes [i nblock]
      (async/go
        (Thread/sleep 250)
        (async/>! c i)))
    (dotimes [i ncompute]
      (async/go
        (async/>! c (factorize (* 29 982451653)))))
    (receive-n c (+ nblock ncompute))))

(defn launch-thread-blocking-and-compute
  [nblock ncompute]
  (let [c (async/chan)]
    (dotimes [i nblock]
      (async/thread
        (Thread/sleep 250)
        (async/>!! c i)))
    (dotimes [i ncompute]
      (async/thread
        (async/>!! c (factorize (* 29 982451653)))))
    (receive-n c (+ nblock ncompute))))
