(ns cljs.client2
  (:require
   [cljs.client :refer [get-today! ok! fail! mean]]
   [devtools.core :as devtools]
   [ajax.core :as http]
   [clojure.string :as string]
   [cljs.test :refer [deftest is run-all-tests run-tests]]))

(devtools/install!)

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

(defn get-price! [date symbol cb eb]
  (if symbol
    (http/GET "http://localhost:3333/price"
      {:handler (fn [response]
                  (-> response
                      js/parseFloat
                      cb))
       :error-handler eb
       :params {:symbol (-> symbol
                            string/upper-case
                            string/trim)
                :date (-> (or date (get-today!))
                          string/trim
                          (string/replace #"/" "-"))}})
    (cb nil)))

(comment
  (get-price! "2018-12-28" "GOOGL" ok! fail!)
  (get-price! "2018/12/28" "GOOGL" ok! fail!)

  (get-price! "2018-12-28" "GOOGL" println

              println))

(deftest test-get-price
  ;; does nothing if symbol is missing
  (get-price! "2018-12-25" nil
              #(is (= nil %))
              #(is false "should not get here"))

  ;; calls callback with result
  (with-redefs [http/GET (fn [url {:keys [handler]}]
                           (handler "1012.12"))]
    (get-price! "2018-12-25"
                "GOOGL"
                #(is (= 1012.12 %))
                #(is false "should not get here")))

  ;; trims symbol
  (with-redefs [http/GET (fn [url {:keys [params]}]
                           (is (= "GOOGL" (-> params :symbol))))]
    (get-price! "2018-12-25" "  googl  " #() #()))

  ;; formats date
  (with-redefs [http/GET (fn [url {:keys [params]}]
                           (is (= "2018-12-25" (-> params :date))))]
    (get-price! " 2018/12/25 " "GOOGL" #() #()))

  ;; uses today if date not present
  (with-redefs [get-today! (fn []
                             "2018-12-28")

                http/GET (fn [url {:keys [params]}]
                           (is (= "2018-12-28" (-> params :date))))]
    (get-price! nil "GOOGL" #() #())))

(run-tests 'cljs.client2)






