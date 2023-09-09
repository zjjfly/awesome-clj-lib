(ns awesome.nippy.examples
  (:require [taoensso.nippy :as nippy]
            [taoensso.encore :as enc])
  (:import (clojure.lang PersistentQueue)
           (java.util UUID Date)
           (taoensso.nippy StressRecord)
           (java.io FileOutputStream DataOutputStream DataInputStream FileInputStream)))

;nippy是一个序列化库,是clojure性能最好的序列化库.
;它是为了弥补clojure原生的reader(read-string函数)的不足,后者在读取大体量的数据的时候非常慢

(def stress-data {:bytes          (byte-array [(byte 1) (byte 2) (byte 3)])
                  :nil            nil
                  :true           true
                  :false          false
                  :char           \ಬ
                  :str-short      "ಬಾ ಇಲ್ಲಿ ಸಂಭವಿಸ"
                  :str-long       (apply str (range 1000))
                  :kw             :keyword
                  :kw-ns          ::keyword
                  :sym            'foo
                  :sym-ns         'foo/bar
                  :regex          #"^(https?:)?//(www\?|\?)?"

                  :queue          (-> (PersistentQueue/EMPTY) (conj :a :b :c :d :e :f :g))
                  :queue-empty1   (PersistentQueue/EMPTY)
                  :queue-empty2   (enc/queue)
                  :sorted-set     (sorted-set 1 2 3 4 5)
                  :sorted-map     (sorted-map :b 2 :a 1 :d 4 :c 3)

                  :list           (list 1 2 3 4 5 (list 6 7 8 (list 9 10)))
                  :list-quoted    '(1 2 3 4 5 (6 7 8 (9 10)))
                  :list-empty     (list)
                  :vector         [1 2 3 4 5 [6 7 8 [9 10]]]
                  :vector-empty   []
                  :map            {:a 1 :b 2 :c 3 :d {:e 4 :f {:g 5 :h 6 :i 7}}}
                  :map-empty      {}
                  :set            #{1 2 3 4 5 #{6 7 8 #{9 10}}}
                  :set-empty      #{}
                  :meta           (with-meta {:a :A} {:metakey :metaval})
                  :nested         [#{{1 [:a :b] 2 [:c :d] 3 [:e :f]} [] #{:a :b}}
                                   #{{1 [:a :b] 2 [:c :d] 3 [:e :f]} [] #{:a :b}}
                                   [1 [1 2 [1 2 3 [1 2 3 4 [1 2 3 4 5]]]]]]

                  :lazy-seq       (repeatedly 1000 rand)
                  :lazy-seq-empty (map identity '())

                  :byte           (byte 16)
                  :short          (short 42)
                  :integer        (int 3)
                  :long           (long 3)
                  :bigint         (bigint 31415926535897932384626433832795)

                  :float          (float 3.14)
                  :double         (double 3.14)
                  :bigdec         (bigdec 3.1415926535897932384626433832795)

                  :ratio          22/7
                  :uuid           (UUID/randomUUID)
                  :date           (Date.)

                  :stress-record  (StressRecord. "data")

                  ;; Serializable
                  :throwable      (Throwable. "Yolo")
                  :exception      (try (/ 1 0) (catch Exception e e))
                  :ex-info        (ex-info "ExInfo" {:data "data"})})

;序列化
(def frozen-stress-data (nippy/freeze stress-data))
;反序列化
(nippy/thaw frozen-stress-data)

;更底层的序列化和反序列化方法
(nippy/freeze-to-out! (DataOutputStream. (FileOutputStream. "./test.data")) {:a 1 :b 2})
(nippy/thaw-from-in! (DataInputStream. (FileInputStream. "./test.data")))
;{:a 1, :b 2}

;加密序列化
(def encrypted-data (nippy/freeze stress-data {:password [:salted "my-password"]}))
(nippy/thaw encrypted-data {:password [:salted "my-password"]})

;自定义类型的序列化和反序列化
(defrecord MyType [data])

(nippy/extend-freeze MyType :my-type/foo                    ; A unique (namespaced) type identifier
                     [x data-output]
                     (.writeUTF data-output (:data x)))

(nippy/extend-thaw :my-type/foo                             ; Same type id
                   [data-input]
                   (MyType. (.readUTF data-input)))

(nippy/thaw (nippy/freeze (MyType. "Joe")))
;#awesome.nippy.examples.MyType{:data "Joe"}

