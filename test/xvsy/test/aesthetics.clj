(ns xvsy.test.aesthetics
  (:require [clojure.test :refer :all]
            [xvsy.aesthetics :refer :all]
            [xvsy.plot :as plot]
            [xvsy.ggsql :as ggsql]))

(def g {:ent {:table "flights", :name "flights", :pk :id, :db {:host "127.0.0.1", :db "airlines", :classname "org.postgresql.Driver", :subprotocol "postgresql", :subname "//127.0.0.1:5432/airlines", :make-pool? true}, :transforms (), :prepares (), :fields [], :rel {}}, :where [], :group [], :table "flights", :db {:host "127.0.0.1", :db "airlines", :classname "org.postgresql.Driver", :subprotocol "postgresql", :subname "//127.0.0.1:5432/airlines", :make-pool? true}, :fields [:korma.core/*], :joins [], :type :select, :alias nil, :modifiers [], :aesthetics {}, :from [{:table "flights", :name "flights", :pk :id, :db {:host "127.0.0.1", :db "airlines", :classname "org.postgresql.Driver", :subprotocol "postgresql", :subname "//127.0.0.1:5432/airlines", :make-pool? true}, :transforms (), :prepares (), :fields [], :rel {}}], :order [], :options nil, :aliases #{}, :results :results})
