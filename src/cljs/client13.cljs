(ns cljs.client13
  (:require
    [cljs.client :refer [get-today! ok! fail! mean usd->num get+]]
    [ajax.core :as http]
    [clojure.string :as string]
    [cljs.reader :as reader]))

;; use tools to improve readability

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

(defn price-req [date today symbol]
  (if symbol
    {:action :get
     :url "http://localhost:3333/price"
     ;; TODO - don't put handler or error-handler here
     :params {:symbol (-> symbol string/upper-case string/trim)
              :date (-> (or date today) string/trim (string/replace #"/" "-"))}}
    {:action :noop}))

(defn price-reqs [dates symbol]
  (->> dates
       (map #(price-req % nil symbol))
       (remove #(= :noop (:action %)))))

;; TODO - collapse into prior function
(defn price-reqs2 [dates-str symbol]
  (price-reqs (reader/read-string dates-str) symbol))

(defn dates-req [looked-up-symbol provided-symbol]
  {:url "http://localhost:3333/dates-available"
   :params {:symbol (or looked-up-symbol provided-symbol)}})

(defn symbol-req [name]
  {:url "http://localhost:3333/symbol"
   :params {:name name}})

(defn get!
  ([req cb]
   (get! req cb cb))
  ([req cb eb]
   (http/GET (:url req)
     (assoc req
            :handler cb
            :error-handler eb))))

(defn response->sym [response]
  (if (and (map? response) (= 404 (:status response)))
    nil
    response))


(defn price-reqs7 [dates-str symbol]
  (price-reqs (reader/read-string dates-str) symbol))

(defn dates-req7 [looked-up-symbol provided-symbol]
  {:url "http://localhost:3333/dates-available"
   :params {:symbol (or looked-up-symbol provided-symbol)}})

(defn symbol-req7 [name]
  {:url "http://localhost:3333/symbol"
   :params {:name name}})

(defn get!7
  ([req cb]
   (get! req cb cb))
  ([req cb eb]
   (http/GET (:url req)
     (assoc req
            :handler cb
            :error-handler eb))))

(defn get+7 [req]
  (let [{:keys [url params]} req]
    (js/Promise. (fn [resolve reject]
                   (http/GET url
                     {:params params
                      :handler resolve
                      :error-handler reject})))))

;; TODO - rename
(defn get+7-both-paths [req]
  (let [{:keys [url params]} req]
    (js/Promise. (fn [resolve reject]
                   (http/GET url
                     {:params params
                      :handler resolve
                      :error-handler resolve})))))

(defn response->sym! [response]
  (if (and (map? response) (= 404 (:status response)))
    nil
    response))

(defn get-low-price! [name symbol cb eb]
  (-> (get+7-both-paths (symbol-req name))
      (.then (fn [response]
               (let [looked-up-symbol (response->sym response)
                     req (dates-req7 looked-up-symbol symbol)]
                 (js/Promise.all [(get+7 req) (js/Promise.resolve req)]))))
      (.then (fn [[dates req]]
               (let [reqs (price-reqs7 dates (-> req :params :symbol))
                     ps (map get+7 reqs)]
                 (js/Promise.all ps))))
      (.then #(map usd->num %))
      (.then #(apply min %))
      (.then cb)
      (.catch eb)))

(comment
  (get-low-price! "Google" nil ok! fail!)
  (get-low-price! nil "GOOGL" ok! fail!)
  (get-low-price! nil nil ok! fail!))
