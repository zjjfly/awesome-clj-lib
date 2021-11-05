(ns com.github.zjjfly.awesome.spec.fake
  (:require [clojure.spec.alpha :as spec]
            [clojure.spec.gen.alpha :as spec-gen]
            [clojure.string :as string]
            [clojure.java.io :as io]
            ))

(spec/def ::email-address string?)
(spec-gen/generate (spec/gen ::email-address))

(defn read-template
  []
  (let [spec (read-string
               (string/replace
                 (slurp (io/resource "template.json")) #":" ""))]
    (for [[k v] spec]
      (vector k (str v)))))

(defn gen-spec
  []
  (let [template (read-template)]
    (doseq [[k v] template]
      ;def key specs
      (eval (read-string (str "(spec/def ::" k " " v ")"))))
    ;def map spec
    (eval (read-string (str "(spec/def ::map-spec (spec/keys :req-un ["
                            (string/join " "
                                         (for [[k _] template]
                                           (str "::" k)))
                            "]))")))))

(def generator (spec/gen (gen-spec)))

(defn reload []
  (let [gen-name (symbol (name 'generator))]
    (intern *ns* gen-name (spec/gen (gen-spec)))))

(defn generate
  [x]
  (spec-gen/generate @x))

(reload)

(generate #'generator)

(comment
  (spec/describe ::map-spec))

(comment
  (spec/valid? ::map-spec {:a "a" :b "a"}))

(comment
  (spec/explain ::map-spec {:a "1" :b "a"}))

