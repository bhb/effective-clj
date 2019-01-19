(ns cljs.client9
  (:require
    [cljs.client :refer [get-today! ok! fail! mean get+]]
    [ajax.core :as http]
    [clojure.string :as string]
    [cljs.reader :as reader]))

;;; what about promises??
;; TODO - maybe use get+ more places

(defn get-price! [date symbol cb eb]
  (if symbol
    (http/GET "http://localhost:3333/price"
              {:handler cb
               :error-handler eb
               :params {:symbol (-> symbol string/upper-case string/trim)
                        :date (-> (or date (get-today!)) string/trim (string/replace #"/" "-"))}})
    (cb nil)))

;; TODO - maybe write get+ instead here
(defn get-price+ [date symbol]
  (js/Promise. (fn [resolve reject]
                 (get-price!
                  date
                  symbol
                  resolve
                  reject))))

(comment
  (-> (get+ "http://localhost:3333/dates-available" {:symbol "GOOGL"})
      (.then reader/read-string)
      (.then ok!))

  )

(defn get-prices+ [symbol dates]
  (let [ps (map #(get-price+ % symbol) dates)]
    (-> (js/Promise.all ps)
        (.then (fn [xs] (map js/parseFloat xs)))
        (.then #(apply min %)))))

(defn get-min-price-available+ [symbol]
  (-> (get+ "http://localhost:3333/dates-available" {:symbol symbol})
      (.then (fn [dates]
               (let [most-recent (->> (reader/read-string dates) sort (take 5))]
                 (get-prices+ symbol most-recent))))))

(defn get-low-price! [name symbol cb eb]
  (-> (if symbol
        (get-min-price-available+ symbol)
        (when name
          (-> (get+ "http://localhost:3333/symbol" {:name name})
              (.then #(get-min-price-available+ %)))))
      (.then cb)
      (.catch eb)))

(comment
  (get-low-price! "Google" nil ok! fail!)
  (get-low-price! nil "GOOGL" ok! fail!))

;; what'd we get?
;; -- shorter code
;; -- less repetition of callbacks
;; -- less nesting
;; -- still no transparency
;; -- TODO - maybe eventually do version with promises and, say, circuit-breaker?

;; if not abstraction, or tools, what helps?
