(ns cljs.client11
  (:require
    [cljs.client :refer [get-today! ok! fail! mean usd->num get+]]
    [ajax.core :as http]
    [clojure.string :as string]
    [cljs.reader :as reader]))

;; 2. extract pure functions

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

(defn get+2 [req]
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
            (let [reqs (price-reqs2 dates (-> req :params :symbol))
                  ps (map get+2 reqs)]
              (-> (js/Promise.all ps)
                  ;; TODO - extra min and usd->num into function
                  (.then #(map usd->num %))
                  (.then #(apply min %))
                  (.then cb)
                  (.catch eb))))
          eb)))

(defn response->sym [response]
  (if (and (map? response) (= 404 (:status response)))
    nil
    response))

(defn get-low-price! [name symbol cb eb]
  (get! (symbol-req name)
        #(get-prices! (response->sym %) symbol cb eb)))

(comment
  (price-reqs2 "[\"2018-12-01\"]" "GOOGL")
  (get-low-price! "Google" nil ok! fail!)
  (get-low-price! nil "GOOGL" ok! fail!))
