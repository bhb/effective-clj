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
    (http/GET "http://localhost:3333/price" {:handler (fn [response] (cb
                                                                      (js/parseFloat
                                                                       (string/replace response
                                                                                       " USD" ""))))
                                             :error-handler (fn [response] (cb nil))
                                             :params {:symbol (-> symbol string/upper-case string/trim)
                                                      :date (-> (or date (today)) string/trim (string/replace #"/" "-"))}})
    (cb nil)))

(comment
  (get-price "2018-12-28" "GOOGL" (fn [x] (prn x)))
  (get-price "2018/12/28" "googl" (fn [x] (prn x)))

  
  )



















