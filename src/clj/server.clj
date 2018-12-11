(ns server
  (:require [org.httpkit.server :as httpkit]
            [ring.middleware.params :refer [wrap-params]]))

(def company-name->symbol
  {"google" "GOOGL"})

(defn app* [req]
  (let [{:keys [uri query-params]} req
        {:strs [name]} query-params]
    (case uri
      "/symbol"
      (if-let [stock-symbol (get company-name->symbol name)]
        {:status  200
         :headers {"Content-Type" "text/plain"}
         :body    stock-symbol}
        {:status  404
         :headers {"Content-Type" "text/plain"}
         :body    (str "No symbol found for " name)})

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
