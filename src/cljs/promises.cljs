(ns cljs.promises
  (:require
    [ajax.core :as http]))

(defn get-symbol! [name cb]
  (http/GET "http://localhost:3333/symbol"
            {:params {:name name}
             :handler cb}))

(defn get-price! [symbol date cb]
  (http/GET "http://localhost:3333/price"
            {:params {:symbol symbol
                      :date date}
             :handler cb}))

(comment
  (get-symbol! ["Google"]
               (fn [symbol]
                 (get-price! symbol "2018-12-27"
                             (fn [price]
                               (println "$" price)))))
  )

(defn get-symbol+ [name date]
  (js/Promise. (fn [resolve reject]
                 (get-symbol! name resolve))))

(defn get-price+ [symbol date]
  (js/Promise. (fn [resolve reject]
                 (get-price! symbol date resolve))))

(comment
  (-> (get-symbol+ "Google")
      (.then #(get-price+ % "2018-12-27"))
      (.then #(println "$" %)))
  )

