(ns cljs.client5
  (:require
    [cljs.client :refer [get-today! ok! fail! mean]]
    [ajax.core :as http]
    [clojure.string :as string]))

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

(comment
  (-> (get-price+ "2018-12-27" "GOOGL")
      (.then ok!)
      (.catch fail!)))

;; Step 1, break into independent effects

(defn get-mean-price! [dates symbol cb eb]
  (let [ps (map #(get-price+ % symbol) dates)]
    (-> (js/Promise.all ps)
        (.then (fn [xs] (map js/parseFloat xs)))
        (.then mean)
        (.then cb)
        (.catch eb))))

(comment
  (get-mean-price! ["2018-12-26" "2018-12-27" "2018-12-28"] "GOOGL" ok! fail!)

  )
