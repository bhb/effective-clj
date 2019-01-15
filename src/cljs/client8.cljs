(ns cljs.client8
  (:require
    [cljs.client :refer [get-today! ok! fail! mean usd->num]]
    [ajax.core :as http]
    [clojure.string :as string]
    [cljs.reader :as reader]))

;;;;;;;;;;;;;;;;;;;

;; first, show the parts that are logic and parts that are effects
;; to show that it's not trivial to break it out
;; emphasize we can't trivially use our old trick

;; what about abstraction?
;; abstract into functions!
;; (do a checklist)
;; cool, we've improved duplication, code is shorter
;; have we made it more transparent? 
;; -- show that we haven't actually made our code more testable

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

(defn get-prices! [symbol dates cb eb]
  (let [ps (map #(get-price+ % symbol) dates)]
    (-> (js/Promise.all ps)
        (.then (fn [xs] (map usd->num xs)))
        (.then #(apply min %))
        (.then cb)
        (.catch eb))))

(defn get-min-price-available! [symbol cb eb]
  (http/GET "http://localhost:3333/dates-available"
    {:params {:symbol symbol}
     :error-handler eb
     :handler (fn [dates]
                (get-prices! symbol (reader/read-string dates) cb eb))}))

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
