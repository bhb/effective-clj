(ns cljs.client3
  (:require
   [cljs.client :refer [get-today! ok! fail! mean]]
   [devtools.core :as devtools]
   [ajax.core :as http]
   [clojure.string :as string]))

(devtools/install!)

(defn price-req [date today symbol]
  (if symbol
    {:action :get
     :url "http://localhost:3333/price"
     :params {:symbol (-> symbol string/upper-case string/trim)
              :date (-> (or date today) string/trim (string/replace #"/" "-"))}}
    {:action :noop}))

(comment

  (price-req nil "2018-12-29" nil) ; {:action :noop}
  (price-req nil "2018-12-29" "googl") ; {:action :get, :url "http://localhost:3333/price", :params {:symbol "GOOGL", :date "2018-12-29"}}
  (price-req "2018-12-28" "2018-12-29" "GOOGL") ; {:action :get, :url "http://localhost:3333/price", :params {:symbol "GOOGL", :date "2018-12-28"}}

  (let [req' (assoc (price-req "2018-12-28" "2018-12-29" "GOOGL")
                    :handler #(ok! (usd->num %)))]
    ((:handler req') "100 USD")))

(defn get-price! [date symbol cb eb]
  (let [req (price-req date (get-today!) symbol)]
    (case (:action req)
      :get (let [req' (assoc req
                             :handler #(cb (js/parseFloat %))
                             :error-handler eb)]
             (http/GET (:url req') req'))
      :noop (cb nil))))

(comment
  (get-price! "2018-12-28" "GOOGL" ok! fail!)
  (get-price! "2018/12/28" "googl" ok! fail!)
  (get-price! "2018/12/28" nil ok! fail!))
