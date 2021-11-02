(ns build
  (:require [clojure.tools.build.api :as b]
            [clojure.java.io :as io]
            [clojure.string :as s]
            ))

(def lib ::awesome-clj)
;(def version (format "1.2.%s" (b/git-count-revs nil)))
(def version "1.0")
(def clj-source "src/main/clj")
(def java-source "src/main/java")
(def resources "src/main/resources")
(def target-dir "target")
(def class-dir "target/classes")
(def basis (b/create-basis {:project "deps.edn"}))
(def jar-file (format "target/%s-%s.jar" (name lib) version))
(def uber-file (format "target/%s-%s-standalone.jar" (name lib) version))

(defn java-file-exist?
  [dir]
  (let [files (file-seq (io/file dir))]
    (some #(s/ends-with? (.getName %) ".java")
          (filter #(.isFile %) files))))

(defn clean
  "Delete the build target directory"
  [_]
  (println (str "Cleaning " target-dir))
  (b/delete {:path target-dir}))

(defn prep [_]
  (b/write-pom {:class-dir class-dir
                :lib       lib
                :version   version
                :basis     basis
                :src-dirs  [java-source clj-source]})
  (b/copy-dir {:src-dirs   [resources]
               :target-dir class-dir}))

(defn jar [_]
  (clean _)
  (prep _)
  (when (java-file-exist? java-source)
    (b/javac {:src-dirs   [java-source]
              :class-dir  class-dir
              :basis      basis
              :javac-opts ["-source" "8" "-target" "8"]}))
  (b/compile-clj {:basis     basis
                  :src-dirs  [clj-source]
                  :class-dir class-dir})
  (b/jar {:class-dir class-dir
          :jar-file  jar-file
          :main      nil}))

(defn uber [_]
  (clean _)
  (prep _)
  (when (java-file-exist? java-source)
    (b/javac {:src-dirs   [java-source]
              :class-dir  class-dir
              :basis      basis
              :javac-opts ["-source" "8" "-target" "8"]}))
  (b/compile-clj {:basis     basis
                  :src-dirs  [clj-source]
                  :class-dir class-dir})
  (b/uber {:class-dir class-dir
           :uber-file uber-file
           :basis     basis}))
