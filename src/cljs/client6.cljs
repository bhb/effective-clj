(ns cljs.client6
  (:require
    [cljs.client :refer [get-today! ok! fail! mean usd->num]]
    [ajax.core :as http]
    [clojure.string :as string]))

(defn price-req [date today symbol]
  (if symbol
    {:action :get
     :url "http://localhost:3333/price"
     :params {:symbol (-> symbol string/upper-case string/trim)
              :date (-> (or date today) string/trim (string/replace #"/" "-"))}}
    {:action :noop}))

(defn price-reqs [dates symbol]
  (->> dates
       (map #(price-req % nil symbol))
       (remove #(= :noop (:action %)))))

(defn mean-price [prices-str]
  (->> prices-str
       (map js/parseFloat)
       mean))

(comment
  (price-reqs [] nil)
  (price-reqs ["2018-12-28"] nil)
  (price-reqs ["2018-12-28"] "GOOGL"))

(defn get-price+ [req]
  (js/Promise. (fn [resolve reject]
                 (http/GET (:url req)
                           (assoc req
                                  :handler resolve
                                  :error-handler reject)))))

(defn get-mean-price! [dates symbol cb eb]
  (let [reqs (price-reqs dates symbol)
        ps (map get-price+ reqs)]
    (-> (js/Promise.all ps)
        (.then mean-price)
        (.then cb)
        (.catch eb))))

(comment
  (get-mean-price! ["2018-12-26" "2018-12-27" "2018-12-28"] "GOOGL" ok! fail!)

  )
