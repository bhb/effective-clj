(ns cljs.client14
  (:require
    [cljs.client :refer [get-today! ok! fail!]]
    [ajax.core :as http]
    [clojure.string :as string]
    [cljs.reader :as reader]
    [cognitect.transit :as t]))

;; finally, consider if builing bigger requests

(defn price-req [date today symbol]
  (if symbol
    {:action :get
     :url "http://localhost:3333/price"
     ;; TODO - don't put handler or error-handler here
     :params {:symbol (-> symbol string/upper-case string/trim)
              :date (-> (or date today) string/trim (string/replace #"/" "-"))}}
    {:action :noop}))

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

(defn prices-req [symbol dates]
  {:url "http://localhost:3333/symbolz"
   :params {:symbol symbol :dates (pr-str dates)}})

(defn min-price [m-str]
  (->> (reader/read-string m-str)
       vals
       (map js/parseFloat)
       (apply min)))

(defn get+ [req]
  (let [{:keys [url params]} req]
    (js/Promise. (fn [resolve reject]
                   (http/GET url
                     {:params params
                      :handler resolve
                      :error-handler reject})))))

(defn get-no-fail+ [req]
  (let [{:keys [url params]} req]
    (js/Promise. (fn [resolve reject]
                   (http/GET url
                     {:params params
                      :handler resolve
                      :error-handler resolve})))))

(defn get-low-price! [name symbol cb eb]
  (-> (get-no-fail+ (symbol-req name))
      (.then (fn [response]
               (let [looked-up-symbol (response->sym response)
                     req (dates-req looked-up-symbol symbol)]
                 (js/Promise.all [(get+ req) (js/Promise.resolve req)]))))
      (.then (fn [[dates-str req]]
               (get+ (prices-req (-> req :params :symbol) (reader/read-string dates-str)))))
      (.then min-price)
      (.then cb)
      (.catch eb)))

(comment
  (get-low-price! "Google" nil ok! fail!)
  (get-low-price! nil "GOOGL" ok! fail!)
  (get-low-price! nil nil ok! fail!))
