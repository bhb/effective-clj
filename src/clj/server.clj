(ns server
  (:require [org.httpkit.server :as httpkit]
            [ring.middleware.params :refer [wrap-params]]
            [clojure.string :as string]))

(def company-name->symbol
  {"google" "GOOGL"})

;; opening prices
(def symbol->date->price
  {"GOOGL" {"2018-12-28" 1059.50
            "2018-12-27" 1052.90
            "2018-12-26" 1051.10
            "2018-12-25" 1060.01}})

(defn app* [req]
  (let [{:keys [uri query-params]} req
        {:strs [name symbol date]} query-params]
    (case uri
      "/symbol"
      (if-let [stock-symbol (get company-name->symbol (string/lower-case name))]
        {:status  200
         :headers {"Content-Type" "text/plain"
                   "Access-Control-Allow-Origin" "*"}
         :body    stock-symbol}
        {:status  404
         :headers {"Content-Type" "text/plain"
                   "Access-Control-Allow-Origin" "*"}
         :body    (str "No symbol found for " name)})

      "/dates-available"
      (if-let [date->price (get symbol->date->price symbol)]
        {:status  200
         :headers {"Content-Type" "text/plain"
                   "Access-Control-Allow-Origin" "*"}
         :body    (-> date->price keys sort vec pr-str)}
        {:status  404
         :headers {"Content-Type" "text/plain"
                   "Access-Control-Allow-Origin" "*"}
         :body    (str "No dates found for " symbol)})

      "/price"
      (if-let [price (get-in symbol->date->price [symbol date])]
        {:status  200
         :headers {"Content-Type" "text/plain"
                   "Access-Control-Allow-Origin" "*"}
         :body    (str price " USD")}
        {:status  404
         :headers {"Content-Type" "text/plain"
                   "Access-Control-Allow-Origin" "*"}
         :body    (str "No price found for symbol " symbol " on date " date)})
      {:status 404
       :body "Not found"})))

(def app (wrap-params app*))

(comment
  (app {:request-method :get
        :uri "/symbol"
        :query-string "name=google"})
  (app {:request-method :get
        :uri "/symbol"
        :query-string "name=google1"}))

(defn start [port]
  (httpkit/run-server (wrap-params app) {:port port})
  (println "server running on port" port))

(defn -main []
  (start 3333))
