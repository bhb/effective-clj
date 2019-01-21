(ns cljs.client4
  (:require
   [cljs.client :refer [get-today! ok! fail! mean]]
   [ajax.core :as http]
   [clojure.string :as string]))

;;;;;;;;;;;;; independent ;;;;;;;;;;;;;;;;;;;;

(defn get-price! [date symbol cb eb]
  (if symbol
    (http/GET "http://localhost:3333/price"
      {:handler (fn [response]
                  (cb
                   (-> response
                       (string/replace " USD" "")
                       js/parseFloat)))
       :error-handler (fn [response] (eb response))
       :params {:symbol (-> symbol string/upper-case string/trim)
                :date (-> (or date (get-today!)) string/trim (string/replace #"/" "-"))}})
    (cb nil)))

;; First attempt: recursion

(defn get-prices! [dates symbol prices cb eb]
  (if (empty? dates)
    (cb prices)
    (let [[first-date & rest] dates]
      (get-price! first-date
                  symbol
                  (fn [price]
                    (get-prices! rest
                                 symbol
                                 (conj prices price)
                                 cb
                                 eb))
                  eb))))

(defn get-mean-price! [dates symbol cb eb]
  (get-prices! dates
               symbol
               []
               (fn [prices] (cb (mean prices)))
               eb))

(comment
  (get-prices! ["2018-12-26" "2018-12-27" "2018-12-28"] "GOOGL" [] ok! fail!)

  (get-mean-price! ["2018-12-26" "2018-12-27" "2018-12-28"] "GOOGL" ok!



                   fail!))



