(ns cljs.client7
  (:require
   [cljs.client :refer [get-today! ok! fail!]]
   [ajax.core :as http]
   [clojure.string :as string]
   [cljs.reader :as reader]))

;; that's the easy bits

;; 1. given company name, get symbol
;; 2. given symbol, get all prices


;; fundamentally, we have
;; company -> symbol -> dates -> prices

;; attempt 1
;; 1. callbacks

(defn get-price! [date symbol cb eb]
  (if symbol
    (http/GET "http://localhost:3333/price"
      {:handler cb
       :error-handler eb
       :params {:symbol (-> symbol string/upper-case string/trim)
                :date (-> (or date (get-today!)) string/trim (string/replace #"/" "-"))}})
    (cb nil)))

(defn get-price+ [date symbol]
  (js/Promise. (fn [resolve reject]
                 (get-price!
                  date
                  symbol
                  resolve
                  reject))))

(defn get-min-price-available! [symbol cb eb]
  (http/GET "http://localhost:3333/dates-available"
    {:params {:symbol symbol}
     :error-handler eb
     :handler (fn [dates]
                (let [parsed-dates (reader/read-string dates)
                      most-recent (->> parsed-dates sort (take 5))
                      ps (map #(get-price+ % symbol) most-recent)]
                  (-> (js/Promise.all ps)
                      (.then (fn [xs] (map js/parseFloat xs)))
                      (.then #(apply min %))
                      (.then cb)
                      (.catch eb))))}))

(defn get-low-price! [name symbol cb eb]
  (if symbol
    (get-min-price-available! symbol cb eb)
    (when name
      (http/GET "http://localhost:3333/symbol"
        {:params {:name name}
         :error-handler eb
         :handler (fn [symbol]
                    (get-min-price-available! symbol cb eb))}))))

(comment
  (get-low-price! "Google" nil ok! fail!)
  (get-low-price! nil "GOOGL" ok! fail!))
