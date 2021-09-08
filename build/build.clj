(ns build
  (:require [badigeon.javac :as j]
            [badigeon.clean :as clean]))

(defn javac []
      (println "Compiling Java")
      (j/javac "src" {:compile-path     "target/classes"
                      ;; Additional options used by the javac command
                      :compiler-options ["-cp" "src:target/classes" "-target" "1.8"
                                         "-source" "1.8" "-Xlint:-options"]})
      (println "Compilation Completed"))

(defn clean []
  ;; Delete the target directory
  (clean/clean "target"
               {;; By default, Badigeon does not allow deleting folders outside the target directory,
                ;; unless :allow-outside-target? is true
                :allow-outside-target? false}))

(defn -main [tag]
      (let [t (keyword tag)]
       (cond
       (= t :compile ) (javac)
       (= t :clean ) (clean)
       )))
