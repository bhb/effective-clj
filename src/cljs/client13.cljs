(ns cljs.client13
  (:require
    [cljs.client :refer [get-today! ok! fail! mean]]
    [ajax.core :as http]
    [clojure.string :as string]
    [cljs.reader :as reader]))

;; use tools to improve readability

(defn price-req [date today symbol]
  (if symbol
    {:action :get
     :url "http://localhost:3333/price"
     :params {:symbol (-> symbol string/upper-case string/trim)
              :date (-> (or date today) string/trim (string/replace #"/" "-"))}}
    {:action :noop}))

(defn price-reqs [dates-str symbol]
  (->> dates-str
       reader/read-string
       (map #(price-req % nil symbol))
       (remove #(= :noop (:action %)))))

(defn dates-req [looked-up-symbol provided-symbol]
  {:url "http://localhost:3333/dates-available"
   :params {:symbol (or looked-up-symbol provided-symbol)}})

(defn symbol-req [name]
  {:url "http://localhost:3333/symbol"
   :params {:name name}})

(defn min-price [price-strs]
  (apply min (map js/parseFloat price-strs)))

(defn response->sym [response]
  (if (and (map? response) (= 404 (:status response)))
    nil
    response))

(defn get!
  ([req cb]
   (get! req cb cb))
  ([req cb eb]
   (http/GET (:url req)
     (assoc req
            :handler cb
            :error-handler eb))))

(defn get+ [req]
  (let [{:keys [url params]} req]
    (js/Promise. (fn [resolve reject]
                   (http/GET url
                     {:params params
                      :handler resolve
                      :error-handler resolve})))))

(defn get-low-price! [name symbol cb eb]
  (-> (get+ (symbol-req name))
      (.then (fn [response]
               (let [looked-up-symbol (response->sym response)
                     req (dates-req looked-up-symbol symbol)]
                 (js/Promise.all [(get+ req) (js/Promise.resolve req)]))))
      (.then (fn [[dates req]]
               (let [reqs (price-reqs dates (-> req :params :symbol))
                     ps (map get+ reqs)]
                 (js/Promise.all ps))))
      (.then min-price)
      (.then cb)
      (.catch eb)))

(comment
  (get-low-price! "Google" nil ok! fail!)
  (get-low-price! nil "GOOGL" ok! fail!)
  (get-low-price! nil nil ok! fail!))
