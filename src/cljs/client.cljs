(ns cljs.client
  (:require
    [devtools.core :as devtools]
    [ajax.core :as http])
  )

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
      (assoc :count (+ (count m1)
                       (count m2)))))

(baz {} {})



(defn handler [response]
  (prn "hello")
  #_(prn response)
  #_(prn (js->clj response))
  )

(defn error-handler [{:keys [status status-text]}]
  (prn (str "something bad happened: " status " " status-text))
  #_(.log js/console ))


(comment
  (http/GET "http://localhost:3333/symbol" {:handler handler
                                            :error-handler error-handler
                                            :params {:name "google"}
                                            })

  )

;; Example - getting stock price via symbol or ticker









