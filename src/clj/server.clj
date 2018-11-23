(ns server
  (:require [reitit.ring :as ring]
            ;;[reitit.coercion.spec]
            ;;[ring.adapter.jetty :as jetty]
            [org.httpkit.server :as httpkit]
            ))

(defn handler [_]
  {:status 200, :body "ok"})

(defn wrap [handler id]
  (fn [request]
    (update (handler request) :wrap (fnil conj '()) id)))

(def app
  (ring/ring-handler
    (ring/router
     ["/api" {:middleware [[wrap :api]]}
      ["/ping" {:get handler
                :name ::ping}]
      ["/admin" {:middleware [[wrap :admin]]}
       ["/users" {:get handler
                  :post handler}]]])))

(defn start []
  (let [port 3333]
    (httpkit/run-server app {:port port})
    (println "server running on port " port)))

(defn -main []
  (start))
