(ns discounter.core-test
  (:require
   [clojure.test :refer [deftest is testing]]
   [discounter.core :as core :refer [item offer one-pass-strategy parse-table
                                     product]]))

(def example-products-raw
  [[0 "item1" 123 :item "Item 1"]
   [1 "item2" 456 :item "Item 2"]
   [2 "item3" 1   :gram "Item 3"] ; TODO: Switch to floats or manage precision.
   [3 "item4" 1234 :item "Item 4"]
   [4 "item5" 5678 :item "Item 5"]])

(def example-basket-raw
  [[0 1]
   [0 1]
   [1 1]
   [1 1]
   [0 1]
   [0 1]
   [1 1]
   [1 1]
   [1 1]
   [2 1000]])

(def example-offers-raw
  [[0 (partial core/bogof-products [0 1]) "BOGOF on Product 1 and Product 2"]
   [1 (partial core/bogof-product 3) "BOGOF on Product 4"]])

(deftest core-test
  (testing "simple example"
    (is (= 1 
      (let [products (parse-table product example-products-raw)
            basket (parse-table item example-basket-raw)
            offers (parse-table offer example-offers-raw)]
        ; (println (bogof-products [0 1] products basket ))))
        (count (one-pass-strategy products basket offers))))
        )))
