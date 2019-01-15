(ns cljs.client10
  (:require
    [cljs.client :refer [get-today! ok! fail! mean usd->num get+]]
    [ajax.core :as http]
    [clojure.string :as string]
    [cljs.reader :as reader]))

;;;;;;;;;;;;;;;;;;;


;; if not abstraction, or tools, what helps?

;; 1. (consider) avoiding decisions
;; 2. transparent functions
;; 3. co-locate IO
;; 4. Use tools to parallelize, improve readability
;; 5. (consider) using bigger requests (e.g. graphQL)


;;;;;;;;;;;;;;;;;;;;;;;;;

;; 1. avoid conditionals

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


(defn get-prices! [symbol cb eb]
  (http/GET "http://localhost:3333/dates-available"
    {:params {:symbol symbol}
     :error-handler eb
     :handler (fn [dates]
                (let [parsed-dates (reader/read-string dates)
                      ps (map #(get-price+ % symbol) parsed-dates)]
                  (-> (js/Promise.all ps)
                      (.then (fn [xs] (map usd->num xs)))
                      (.then #(apply min %))
                      (.then cb)
                      (.catch eb))))}))

(defn get-low-price! [name symbol cb eb]
  (http/GET "http://localhost:3333/symbol"
    {:params {:name name}
     :error-handler #(get-prices! symbol cb eb)
     :handler #(get-prices! % cb eb)}))

(comment
  (get-low-price! "Google" nil ok! fail!)
  (get-low-price! nil "GOOGL" ok! fail!))
