(ns server
  (:require [org.httpkit.server :as httpkit]
            [ring.middleware.params :refer [wrap-params]]
            [clojure.string :as string]
            [expound.alpha :as expound]
            [clojure.spec.alpha :as s]
            [clojure.edn :as edn]))

(set! s/*explain-out* (expound/custom-printer {:theme :figwheel-theme
                                               :print-specs? false
                                               :show-valid-values? true}))

(def company-name->symbol
  {"google" "GOOGL"})

;; opening prices
(def symbol->date->price
  {"GOOGL" {"2018-12-28" 1059.50
            "2018-12-27" 1052.90
            "2018-12-26" 1051.10
            "2018-12-25" 1060.10
            "2018-12-24" 1010.10
            "2018-12-23" 1023.70
            "2018-12-22" 1022.40}})

(defn app* [req]
  (let [{:keys [uri query-params #_form-params]} req
        {:strs [name symbol date dates]} query-params
        dates (edn/read-string dates)]

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
         :body    (str price)}
        {:status  404
         :headers {"Content-Type" "text/plain"
                   "Access-Control-Allow-Origin" "*"}
         :body    (str "No price found for symbol " symbol " on date " date)})
      {:status 404
       :body "Not found"}

      "/prices"
      (if-let [date->prices (get-in symbol->date->price [symbol])]
        {:status  200
         :headers {"Content-Type" "text/plain"
                   "Access-Control-Allow-Origin" "*"}
         :body    (let [date-set (set dates)]
                    (pr-str (->> date->prices
                                 (filter #(contains? date-set (key %)))
                                 (map (fn [[k v]] [k (str v)]))
                                 (into {}))))}
        {:status  404
         :headers {"Content-Type" "text/plain"
                   "Access-Control-Allow-Origin" "*"}
         :body    (str "No prices found for symbol " symbol)}))))

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
