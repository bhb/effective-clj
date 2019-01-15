(ns cljs.client6
  (:require
    [cljs.client :refer [get-today! ok! fail! mean usd->num]]
    [ajax.core :as http]
    [clojure.string :as string]))

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

(comment
  (price-reqs [] nil)
  (price-reqs ["2018-12-28"] nil)
  (price-reqs ["2018-12-28"] "GOOGL"))

;; TODO - use get+
;; TODO - rewrite
(defn get-price+ [req]
  (js/Promise. (fn [resolve reject]
                 (let [req' (-> req
                                ;; TODO - why assoc twice
                                (assoc :handler resolve)
                                (assoc :error-handler reject))]
                   (prn [:get (:url req')])
                   (http/GET (:url req') req')))))

(defn get-mean-price! [dates symbol cb eb]
  (let [reqs (price-reqs dates symbol)
        ps (map get-price+ reqs)]
    (prn reqs)
    (-> (js/Promise.all ps)
        (.then (fn [xs] (map usd->num xs)))
        (.then mean)
        (.then cb)
        (.catch eb)
        )))

(comment
  (get-mean-price! ["2018-12-26" "2018-12-27" "2018-12-28"] "GOOGL" ok! fail!)

  )
