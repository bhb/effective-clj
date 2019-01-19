(ns cljs.client11
  (:require
    [cljs.client :refer [get-today! ok! fail! mean]]
    [ajax.core :as http]
    [clojure.string :as string]
    [cljs.reader :as reader]))

;; 2. extract pure functions

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

(defn response->sym [response]
  (if (and (map? response) (= 404 (:status response)))
    nil
    response))

(defn min-price [price-strs]
  (prn [:bhb.here])
  (apply min (map js/parseFloat price-strs)))

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
                      :error-handler reject})))))

(defn get-prices! [looked-up-symbol provided-symbol cb eb]
  (let [req (dates-req looked-up-symbol provided-symbol)]
    (get! req
          (fn [dates]
            (let [reqs (price-reqs dates (-> req :params :symbol))
                  ps (map get+ reqs)]
              (-> (js/Promise.all ps)
                  (.then min-price)
                  (.then cb)
                  (.catch eb))))
          eb)))

(defn get-low-price! [name symbol cb eb]
  (get! (symbol-req name)
        #(get-prices! (response->sym %) symbol cb eb)))

(comment
  (price-reqs "[\"2018-12-01\"]" "GOOGL")
  (get-low-price! "Google" nil ok! fail!)
  (get-low-price! nil "GOOGL" ok! fail!))
