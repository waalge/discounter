(ns discounter.core
   [clojure.string :as str]
   [clojure.java.io :as io]
   [clojure.tools.cli :refer [parse-opts]])
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

(def cli-options
  [
   ["-b" "--basket BASKET" "filepath of basket. Expect a csv of rows as (product-id, quantity)"
    :default "./my-basket.csv"
    :validate [#(< 0 % 0x10000) "Must be a number between 0 and 65536"]
    ]
   ["-H" "--hostname HOST" "Remote host"
    :default  "localhost"
    ;; Specify a string to output in the default column in the options summary
    ;; if the default value's string representation is very ugly
    :default-desc "localhost"]
   ;; If no required argument description is given, the option is assumed to
   ;; be a boolean option defaulting to nil
   [nil "--detach" "Detach from controlling process"]
   ["-v" nil "Verbosity level; may be specified multiple times to increase value"
    ;; If no long-option is specified, an option :id must be given
    :id :verbosity
    :default 0
    ;; Use :update-fn to create non-idempotent options (:default is applied first)
    :update-fn inc]
   ["-f" "--file NAME" "File names to read"
    :multi true ; use :update-fn to combine multiple instance of -f/--file
    :default []
    ;; with :multi true, the :update-fn is passed both the existing parsed
    ;; value(s) and the new parsed value from each option
    :update-fn conj]
   ;; A boolean option that can explicitly be set to false
   ["-d" "--[no-]daemon" "Daemonize the process" :default true]
   ["-h" "--help"]])

;; The :default values are applied first to options. Sometimes you might want
;; to apply default values after parsing is complete, or specifically to
;; compute a default value based on other option values in the map. For those
;; situations, you can use :default-fn to specify a function that is called
;; for any options that do not have a value after parsing is complete, and
;; which is passed the complete, parsed option map as it's single argument.
;; :default-fn (constantly 42) is effectively the same as :default 42 unless
;; you have a non-idempotent option (with :update-fn or :assoc-fn) -- in which
;; case any :default value is used as the initial option value rather than nil,
;; and :default-fn will be called to compute the final option value if none was
;; given on the command-line (thus, :default-fn can override :default)

(defn usage [options-summary]
  (->> ["This is my program. There are many like it, but this one is mine."
        ""
        "Usage: program-name [options] action"
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

(defn -main [& args] ())
