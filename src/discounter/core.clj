(ns discounter.core
  (:gen-class))

(defn parse-table [stct raw]
  (map #(apply struct stct %) raw))

(def product
  (create-struct :id :name :cost-per-unit :units :pretty-name))

(defn get-product [product-list product-id]
  (first (filter #(= product-id (% :id)) product-list)))

(def item
  (create-struct :product-id :quantity))

(def priced-item
  (create-struct :product-id :quantity :cost-per-unit :total))

(defn price-item [product item]
  (struct priced-item
          (item :product-id)
          (item :quantity)
          (product :cost-per-unit)
          (* (item :quantity) (product :cost-per-unit))))

(def offer
  (create-struct :id :logic :pretty-name))

(def discount
  (create-struct :offer-id :amount))

(defn price-basket [product-list basket]
  (map #(price-item (get-product product-list (% :product-id)) %) basket))

(defn pretty-item [product-list item]
  (let [p (first (get-product product-list (item :product-id)))]
    (println (format "%s\t%d\t%d\t%d"
                     (p :pretty-name)
                     (item :quantity)
                     (p :cost-per-unit)
                     (* (item :quantity) (p :cost-per-unit))))))

(defn pretty-basket [product-list basket]
  (doseq [item basket] (pretty-item product-list item)))

(defn bogof-product [product-id products basket]
  (let [n-prod (reduce #(if (= (%2 :product-id) product-id) (+ %1 (%2 :quantity)) %1) 0 basket)
        amount (* (quot n-prod 2) ((get-product products product-id) :cost-per-unit))]
    amount))

(defn in? [lst x] (some #(= x %) lst))

(defn bogof-products [product-ids products basket]
  (let [zero (into {} (for [pid product-ids] [pid 0]))
        n-prods (reduce
                 #(if (in? product-ids (%2 :product-id))
                    (update %1 (%2 :product-id) + (%2 :quantity)) %1) zero basket)
        costs (reverse (sort #(compare (first %1) (first %2))
                             (for [pid product-ids]
                               [((get-product products pid) :cost-per-unit) (get n-prods pid)])))
        cost-vec (reduce #(concat %1 (repeat (second %2) (first %2))) [] costs)
        amount (reduce + (take-nth 2 (rest cost-vec)))]
    amount))

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
  [[0 (partial bogof-products [0 1]) "BOGOF on Product 1 and Product 2"]
   [1 (partial bogof-product 3) "BOGOF on Product 4"]])

(defn apply-offer [products basket offer]
  (let [discount (apply (offer :logic) [products basket])]
    (if (> discount 0) 
      [[(offer :id) (offer :pretty-name) discount]]
      []
    )))

(defn one-pass-strategy
  "Offers are applied in order. 
  There is no notion of exclusivity or optimization." 
  [products basket offers]
  (reduce #(concat %1 (apply-offer products basket %2)) [] offers))

(defn -main [& args]
  (let [products (parse-table product example-products-raw)
        basket (parse-table item example-basket-raw)
        offers (parse-table offer example-offers-raw)]
    ; (println (bogof-products [0 1] products basket ))))
    (println (one-pass-strategy products basket offers))))


