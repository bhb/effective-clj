(ns cljs.client
  (:require
   [devtools.core :as devtools]
   [ajax.core :as http]))

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

















