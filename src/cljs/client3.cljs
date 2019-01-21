(ns cljs.client3
  (:require
   [cljs.client :refer [get-today! ok! fail! mean]]
   [devtools.core :as devtools]
   [ajax.core :as http]
   [clojure.string :as string]
   [cljs.test :refer [deftest is run-all-tests run-tests]]))

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

(deftest test-get-price
  ;; noop
  (get-price! "2018-12-25" nil
              #(is (= nil %))
              #(is false "should not get here"))

  ;; normal GET
  (with-redefs [http/GET (fn [url {:keys [handler]}]
                           (handler "1012.12"))]
    (get-price! "2018-12-25"
                "GOOGL"
                #(is (= 1012.12 %))
                #(is false "should not get here"))))

(deftest test-price-req
  ;; noops if symbol
  (is (= {:action :noop}
         (price-req "2018-12-28" "2018-12-29" nil)))

  ;; trims symbols
  (is (= {:action :get,
          :url "http://localhost:3333/price",
          :params {:symbol "GOOGL", :date "2018-12-28"}}
         (:params (price-req "2018-12-28" "2018-12-29" "  googl  "))))

  ;; formats date
  (is (= {:action :get,
          :url "http://localhost:3333/price",
          :params {:symbol "GOOGL", :date "2018-12-27"}}
         (:params (price-req " 2018/12/27 " "2018-12-29" "  googl  "))))

  ;; uses second date if first is not present
  (is (= {:symbol "GOOGL", :date "2018-12-29"}
         (:params (price-req nil "2018-12-29" "  googl  ")))))

(run-tests 'cljs.client3)


