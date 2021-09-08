(ns com.github.zjjfly.awesome.korma.mysql
  (:refer-clojure :exclude [update])
  (:use korma.core
        korma.db))

(defdb mysql-db
       (mysql {:db "test" :user "root" :password "123456"}))

(defentity user
           (table :user)
           (database mysql-db))

(defn save-user! []
  (insert user
          (values {:age 28, :name "jjzi", :sex 1})))

(save-user!)

(defn select-user []
  (select user
          (where {:name "jjzi"})
          (order :age :desc)))

(select-user)

