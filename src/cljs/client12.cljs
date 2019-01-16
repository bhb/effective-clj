(ns cljs.client12
  (:require
    [cljs.client :refer [get-today! ok! fail! mean usd->num get+]]
    [ajax.core :as http]
    [clojure.string :as string]
    [cljs.reader :as reader]))

;; co-locate IO

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

(defn price-reqs6 [dates-str symbol]
  (price-reqs (reader/read-string dates-str) symbol))

(defn dates-req6 [looked-up-symbol provided-symbol]
  {:url "http://localhost:3333/dates-available"
   :params {:symbol (or looked-up-symbol provided-symbol)}})

(defn symbol-req6 [name]
  {:url "http://localhost:3333/symbol"
   :params {:name name}})

(defn get!6
  ([req cb]
   (get! req cb cb))
  ([req cb eb]
   (http/GET (:url req)
     (assoc req
            :handler cb
            :error-handler eb))))

(defn get+6 [req]
  (let [{:keys [url params]} req]
    (js/Promise. (fn [resolve reject]
                   (http/GET url
                     {:params params
                      :handler resolve
                      :error-handler reject})))))

(defn response->sym6 [response]
  (if (and (map? response) (= 404 (:status response)))
    nil
    response))

(defn get-low-price6 [name symbol cb eb]
  (get! (symbol-req name)
        (fn [response]
          (let [looked-up-symbol (response->sym response)
                req (dates-req6 looked-up-symbol symbol)]
            (get! req
                  (fn [dates]
                    (let [reqs (price-reqs6 dates (-> req :params :symbol))
                          ps (map get+6 reqs)]
                      (-> (js/Promise.all ps)
                          (.then #(map usd->num %))
                          (.then #(apply min %))
                          (.then cb)
                          (.catch eb))))
                  eb)))))

(comment
  (price-reqs6 "[\"2018-12-01\"]" "GOOGL")
  (get-low-price6 "Google" nil ok! fail!)
  (get-low-price6 nil "GOOGL" ok! fail!))

;; after relocating code, talk about benefits for error handling, performance
;; 
;; -- we actually have downsides (error handling, redundant effects, bigger API)
