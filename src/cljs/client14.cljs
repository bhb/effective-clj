(ns cljs.client14
  (:require
    [cljs.client :refer [get-today! ok! fail! mean usd->num get+]]
    [ajax.core :as http]
    [clojure.string :as string]
    [cljs.reader :as reader]
    [cognitect.transit :as t]))

;; finally, consider if builing bigger requests

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

(defn price-reqs8 [dates-str symbol]
  (price-reqs (reader/read-string dates-str) symbol))

(defn dates-req8 [looked-up-symbol provided-symbol]
  {:url "http://localhost:3333/dates-available"
   :params {:symbol (or looked-up-symbol provided-symbol)}})

(defn symbol-req8 [name]
  {:url "http://localhost:3333/symbol"
   :params {:name name}})

(defn get!8
  ([req cb]
   (get! req cb cb))
  ([req cb eb]
   (http/GET (:url req)
     (assoc req
            :handler cb
            :error-handler eb))))

(defn get+8 [req]
  (let [{:keys [url params]} req]
    (js/Promise. (fn [resolve reject]
                   (http/GET url
                     {:params params
                      :handler resolve
                      :error-handler reject})))))

;; TODO - rename
(defn get+8-both-paths [req]
  (let [{:keys [url params]} req]
    (js/Promise. (fn [resolve reject]
                   (http/GET url
                     {:params params
                      :handler resolve
                      :error-handler resolve})))))

(defn response->sym8 [response]
  (if (and (map? response) (= 404 (:status response)))
    nil
    response))

(defn post+ [req]
  (let [{:keys [url params format response-format]} req]
    (js/Promise. (fn [resolve reject]
                   (http/POST url
                     {:params params
                      :format format
                      :response-format response-format
                      :handler resolve
                      :error-handler resolve})))))

(defn form-data [m]
  (reduce
   (fn [fd [k v]]
     (.append fd (str k) v)
     fd)
   (js/FormData.)
   m))

(form-data {"a" 1 "b" 2})

(defn transit [x]
  (let [w (t/writer :json)]
    (t/write w x)))

(comment
  #_(http/POST "https://postman-echo.com/post"
      {:params {:message "Hello World"
                :user    "Bob"}
       :handler ok!
       :error-handler fail!})

  (http/GET "http://localhost:3333/symbolz"
    {:params {:symbol "GOOGL" :dates (pr-str ["2018-12-28", "2018-12-27"])}
     :response-format :text
     :handler ok!
     :error-handler fail!})

  (edn/edn-response-format)

  (-> (post+ {:url "http://localhost:3333/prices"
              :body (form-data {:symbol "GOOGL" :dates ["2018-12-27" "2018-12-28"]})
              :response-format (edn/edn-response-format)})

      (.then ok!)
      (.catch fail!))

  (http/POST
    "http://localhost:3333/prices"
    {:params {:symbol "GOOGL" :dates ["2018-12-28"]}
     :handler ok!
     :error-handler fail!})

  (http/POST
    "http://localhost:3333/prices"
    {:params {:symbol "GOOGL" :dates ["2018-12-28"]}
     :format :transit
     :handler ok!
     :error-handler fail!})

  (let [form-data (doto
                   (js/FormData.)
                    (.append "symbol" "GOOGL")
                    (.append "dates" ["2018-12-28"]))]
    (http/POST "http://localhost:3333/prices" {:body form-data
                                               :response-format (http/raw-response-format)
                                               :handler ok!
                                               :error-handler fail!})))

(defn prices-req [symbol dates]
  {:url "http://localhost:3333/symbolz"
   :params {:symbol symbol :dates (pr-str dates)}})

(defn get-low-price! [name symbol cb eb]
  (-> (get+8-both-paths (symbol-req name))
      (.then (fn [response]
               (let [looked-up-symbol (response->sym response)
                     req (dates-req8 looked-up-symbol symbol)]
                 (js/Promise.all [(get+8 req) (js/Promise.resolve req)]))))
      (.then (fn [[dates-str req]]
               (prn [:bhb.dates dates-str])
               (get+8 (prices-req (-> req :params :symbol) (reader/read-string dates-str)))))
      (.then (fn [m]
               (->> (reader/read-string m)
                    vals
                    (map usd->num)
                    (apply min))))
      (.then cb)
      (.catch eb)))

(comment
  (get-low-price! "Google" nil ok! fail!)
  (get-low-price! nil "GOOGL" ok! fail!)
  (get-low-price! nil nil ok! fail!))
