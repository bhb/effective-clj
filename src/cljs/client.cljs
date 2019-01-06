(ns cljs.client
  (:require
   [devtools.core :as devtools]
   [ajax.core :as http]
   [clojure.string :as string]))

(devtools/install!)

;; Effect-ive Clojure

;; Effects

;; Effect$$$

;; Pure function

;; Pure, but opaque

(defn foo [key-fn]
  (juxt key-fn identity))

;; Easy to test? Yes
;; Easy to iterate upon? No
;; Easy to reason about? No
(foo first)
(foo :a)

;; Pure is good, but *transparent* is even better

(defn baz [m1 m2]
  (-> m1
      (into m2)
      (assoc :total-keys (+ (count m1)
                            (count m2)))))

(baz {} {})
(baz {:x 1} {:y 2})

(defn handler [response]
  (prn response))

(defn error-handler [{:keys [status status-text]}]
  (prn (str "something bad happened: " status " " status-text)))

(comment
  (http/GET "http://localhost:3333/symbol" {:handler handler
                                            :error-handler error-handler
                                            :params {:name "google"}})
  
  )

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

(defn today []
  (let [t (js/Date.)
        dd (.getDate t)
        dd (if (< dd 10) (str "0" dd) dd)
        mm (inc (.getMonth t))
        mm (if (< mm 10) (str "0" mm) mm)
        yyyy (.getFullYear t)
        ]
    (str yyyy "-" mm "-" dd)))


(defn get-price [date symbol cb]
  (if symbol
    (http/GET "http://localhost:3333/price"
              {:handler (fn [response]
                          (cb
                           (-> response
                               (string/replace " USD" "")
                               js/parseFloat)))
               :error-handler (fn [response] (cb nil))
               :params {:symbol (-> symbol string/upper-case string/trim)
                        :date (-> (or date (today)) string/trim (string/replace #"/" "-"))}})
    (cb nil)))

(defn price-req [date today symbol cb eb]
  (if symbol
    {:action :get
     :url "http://localhost:3333/price"
     :handler (fn [response]
                (cb
                 (-> response
                     (string/replace " USD" "")
                     js/parseFloat)))
     :error-handler (fn [response]
                      (eb response))
     :params {:symbol (-> symbol string/upper-case string/trim)
              :date (-> (or date today) string/trim (string/replace #"/" "-"))}}
    {:action :noop}))

(comment
  (let [p (js/Promise. (fn [resolve reject] (js/setTimeout (fn [] (resolve "foo")) 1000)))]
    (-> p
        (.then (fn [x] (prn x)))))

  (let [p (js/Promise. (fn [resolve reject]
                         (let [req (price-req nil (today) "googl" resolve reject)]
                           (case (:action req)
                             :get (http/GET (:url req) req)
                             :noop (reject nil)))
                         ))]
    (-> p
        (.then (fn [x] (prn x)))
        (.catch (fn [x] (prn "failed with" x)))
        )
    )
  )


(comment
  (price-req nil "2018-12-29" nil (fn [x] (prn x)) (fn [x] (prn x)))
  (price-req nil "2018-12-29" "googl" (fn [x] (prn x)) (fn [x] (prn x)))
  (price-req "2018-12-28" "2018-12-29" "GOOGL" (fn [x] (prn x)) (fn [x] (prn x)))
  
  )

(defn get-price2 [date symbol cb]
  (let [req (price-req date (today) symbol cb)]
    (case (:action req)
      :get (http/GET (:url req) req)
      :noop (cb nil))))

(comment
  (get-price2 "2018-12-28" "GOOGL" (fn [x] (prn x)))
  (get-price2 "2018/12/28" "googl" (fn [x] (prn x)))
  )

;;;;;;;;;;;;; independent ;;;;;;;;;;;;;;;;;;;;

(defn mean [prices]
  (/ (apply + prices)
     (count prices)))


(defn get-prices [dates symbol prices cb]
  (if (empty? dates)
    (cb prices)
    (let [[first-date & rest] dates]
      (get-price first-date
                 symbol
                 (fn [price]
                   (get-prices
                    rest
                    symbol
                    (conj prices price)
                    cb))))))

(comment
  (get-prices ["2018-12-26" "2018-12-27" "2018-12-28"] "GOOGL" [] (fn [x] (prn [:done x])))
  )

;; First attempt: recursion
(defn get-average-price1 [dates symbol cb]
  
  )


;; TODO - thread reject into request
(defn get-average-price2 [dates symbol cb]
  (let [reqs (map
              #(price-req % nil symbol (fn [resolve reject]
                                         ()
                                         ))
              dates
              )])
  
  (let [p (js/Promise.all [])])
  )



















