(ns discounter.core
   (:require 
    [clojure.string :as str]
    [clojure.data.csv :as csv]
    [clojure.java.io :as io]
    [clojure.tools.cli :refer [parse-opts]]
  )
  (:gen-class))

;; Types 

(def product
  (create-struct :id :name :cost-per-unit :units :pretty-name))

(def item
  (create-struct :product-id :quantity))

(def priced-item
  (create-struct :product-id :quantity :cost-per-unit :total))

(def offer
  (create-struct :id :logic :pretty-name))

(def discount
  (create-struct :offer-id :amount))

(defn in? [lst x] (some #(= x %) lst))

;; Helpers

(defn get-product [product-list product-id]
  (first (filter #(= product-id (% :id)) product-list)))

(defn price-item [product item]
  (struct priced-item
          (item :product-id)
          (item :quantity)
          (product :cost-per-unit)
          (* (item :quantity) (product :cost-per-unit))))

(defn price-basket [product-list basket]
  (map #(price-item (get-product product-list (% :product-id)) %) basket))

(defn parse-table [stct raw]
  (map #(apply struct stct %) raw))

;; Pretty printing 

(defn pretty-item [product-list item]
  (let [p (first (get-product product-list (item :product-id)))]
    (println (format "%s\t%d\t%d\t%d"
                     (p :pretty-name)
                     (item :quantity)
                     (p :cost-per-unit)
                     (* (item :quantity) (p :cost-per-unit))))))

(defn pretty-basket [product-list basket]
  (doseq [item basket] (pretty-item product-list item)))

;; Example types of offers 

(defn bogof-product [product-id products basket]
  (let [n-prod (reduce #(if (= (%2 :product-id) product-id) (+ %1 (%2 :quantity)) %1) 0 basket)
        amount (* (quot n-prod 2) ((get-product products product-id) :cost-per-unit))]
    amount))

(defn bogof-products 
  "bogof with items paired off from most to least expensive"
  [product-ids products basket]
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

;; Strategy 

(defn apply-offer [products basket offer]
  (let [discount 
    (apply (offer :logic) [products basket])
    ]
    (if (> discount 0) 
      [[(offer :id) (offer :pretty-name) discount]]
      []
    )))

(defn one-pass-strategy
  "Offers are applied in order. 
  There is no notion of exclusivity or optimization." 
  [products basket offers]
  (reduce #(concat %1 (apply-offer products basket %2)) [] offers))

;; CLI 

(def cli-options
  [
   ["-b" "--basket BASKET" "filepath of basket. Expect a csv of rows as (product-id, quantity)"
    :default "./my-basket.csv"
    ]
   ["-h" "--help"]])

(defn usage [options-summary]
  (->> ["Apply your offers to your basket"
        ""
        "Usage: discounter [options] action"
        ""
        "Options:"
        options-summary
        ""
        "Actions:"
        "  run      Run"
        ""
        "Please refer to the manual page for more information."]
       (str/join \newline)))

(defn error-msg [errors]
  (str "The following errors occurred while parsing your command:\n\n"
       (str/join \newline errors)))

(defn validate-args
  "Validate command line arguments. Either return a map indicating the program
  should exit (with an error message, and optional ok status), or a map
  indicating the action the program should take and the options provided."
  [args]
  (let [{:keys [options arguments errors summary]} (parse-opts args cli-options)]
    (cond
      (:help options) ; help => exit OK with usage summary
      {:exit-message (usage summary) :ok? true}
      errors ; errors => exit with description of errors
      {:exit-message (error-msg errors)}
      ;; custom validation on arguments
      (and (= 1 (count arguments))
           (#{"run"} (first arguments)))
      {:action (first arguments) :options options}
      :else ; failed custom validation => exit with usage summary
      {:exit-message (usage summary)})))

(defn exit [status msg]
  (println msg)
  (System/exit status))


(defn get-csv [filepath]
  (with-open [reader (io/reader filepath)]
    (doall
      (csv/read-csv reader)))
 )

(defn read-basket 
  "FIXME : I cannot get clojure to sensibly parse this"
  [filepath]
  (into [] (for [row (get-csv filepath)] (struct item (Integer/parseInt (first row)) (Integer/parseInt (second row)))))
 )

(def default-products-raw
  "FIXME : make this import from file"
  [[0 "prod1" 123 :item "Product 1"]
   [1 "prod2" 456 :item "Product 2"]
   [2 "prod3" 1   :gram "Product 3"] ; TODO: Switch to floats or manage precision.
   [3 "prod4" 1234 :item "Product 4"]
   [4 "prod5" 5678 :item "Product 5"]])

(def default-offers-raw
  "FIXME : make this import from file"
  [[0 (partial bogof-products [0 1]) "BOGOF on Product 1 and Product 2"]
   [1 (partial bogof-product 3) "BOGOF on Product 4"]])


(defn runner
  [options]
  (let [products (parse-table product default-products-raw)
        offers (parse-table offer default-offers-raw)
        basket (read-basket (options :basket))
        ]
    (println (one-pass-strategy products basket offers))))

(defn -main [& args]
  (let [{:keys [action options exit-message ok?]} (validate-args args)]
    (if exit-message
      (exit (if ok? 0 1) exit-message)
      (case action
        "run"  (runner options)))))
