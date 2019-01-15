(ns cljs.client2
  (:require
   [cljs.client :refer [get-today! ok! fail! mean]]
   [devtools.core :as devtools]
   [ajax.core :as http]
   [clojure.string :as string]))

(devtools/install!)

;; Simple case - need something that will involve up front computation
;; for HTTP case with several cases.

;; Simple case - if you provide date, will look up stock price at date,
;; otherwise looks up price for today?

;; Slightly more complicated case - date range (sends multiple parallel requests).

;; function would take in today's date, today date,

;; server has one endpoint that takes stock symbol and date and returns
;; price.

;; stock symbol must be present, must be uppercase,
;; if symbol is not provided, return nil
;; if price not found, return nil

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

(comment
  (get-price! "2018-12-28" "GOOGL" ok!

              fail!))




