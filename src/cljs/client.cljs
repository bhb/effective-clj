(ns cljs.client
  (:require
   [devtools.core :as devtools]
   [ajax.core :as http]
   [clojure.string :as string]
   [cljs.reader :as reader]
   [cognitect.transit :as t]))

(devtools/install!)

;;;;;;;;;; helpers ;;;;;;;;;;;;;;;

(defn get-today! []
  (let [t (js/Date.)
        dd (.getDate t)
        dd (if (< dd 10) (str "0" dd) dd)
        mm (inc (.getMonth t))
        mm (if (< mm 10) (str "0" mm) mm)
        yyyy (.getFullYear t)]
    (str yyyy "-" mm "-" dd)))

(defn ok! [x]
  (prn [:ok x]))

(defn fail! [x]
  (prn [:fail x]))

(defn mean [prices]
  (/ (apply + prices)
     (count prices)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn get-price! [date symbol cb eb]
  (if symbol
    (http/GET "http://localhost:3333/price"
      {:handler (fn [response]
                  (cb
                   (-> response
                       (string/replace " USD" "")
                       js/parseFloat)))
       :error-handler (fn [response] (eb response))
       :params {:symbol (-> symbol string/upper-case string/trim)
                :date (-> (or date (get-today!)) string/trim (string/replace #"/" "-"))}})
    (cb nil)))

(defn price-req [date today symbol]
  (if symbol
    {:action :get
     :url "http://localhost:3333/price"
     ;; TODO - don't put handler or error-handler here
     :params {:symbol (-> symbol string/upper-case string/trim)
              :date (-> (or date today) string/trim (string/replace #"/" "-"))}}
    {:action :noop}))

;; TODO - maybe write get+ instead here
(defn get-price+ [date symbol]
  (js/Promise. (fn [resolve reject]
                 (get-price!
                  date
                  symbol
                  resolve
                  reject))))

(comment
  (-> (get-price+ "2017-12-26" "GOOGL")
      (.then ok!)))

;; Step 1, break into independent effects

(defn get-mean-price2 [dates symbol cb eb]
  (let [ps (map #(get-price+ % symbol) dates)]
    (-> (js/Promise.all ps)
        (.then mean)
        (.then cb)
        (.catch eb))))

(comment
  (get-mean-price2 ["2018-12-26" "2018-12-27" "2018-12-28"] "GOOGL" ok! fail!))

(defn price-reqs [dates symbol]
  (->> dates
       (map #(price-req % nil symbol))
       (remove #(= :noop (:action %)))))

(comment
  (price-reqs [] nil)
  (price-reqs ["2018-12-28"] nil)
  (price-reqs ["2018-12-28"] "GOOGL"))

(defn request+ [req])

(defn get-price+2 [req]
  (js/Promise. (fn [resolve reject]
                 (let [req' (-> req
                                (update :handler #(comp resolve %))
                                (update :handler #(comp reject %)))]
                   (http/GET (:url req') req')))))

(defn get-mean-price3 [dates symbol cb eb]
  (let [reqs (price-reqs dates symbol)
        ps (map get-price+2 reqs)]
    (-> (js/Promise.all ps)
        (.then mean)
        (.then cb)
        (.catch eb))))

(comment
  (get-mean-price3 ["2018-12-26" "2018-12-27" "2018-12-28"] "GOOGL" ok! fail!))

(comment
  (let [p (js/Promise. (fn [resolve reject] (js/setTimeout (fn [] (resolve "foo")) 1000)))]
    (-> p
        (.then (fn [x] (prn x)))))

  (let [p (js/Promise. (fn [resolve reject]
                         (let [req (price-req nil (get-today!) "googl" resolve reject)]
                           (case (:action req)
                             :get (http/GET (:url req) req)
                             :noop (reject nil)))))]
    (-> p
        (.then (fn [x] (prn x)))
        (.catch (fn [x] (prn "failed with" x))))))

;; that's the easy bits

;; 1. given company name, get symbol
;; 2. given symbol, get all prices



;; what if API adds API that is multi-read for dates? graphql is example
;;

;; fundamentally, we have
;; company -> symbol -> dates -> prices

;; 1. callbacks


(defn get-low-price [name symbol cb eb]
  (if symbol
    (http/GET "http://localhost:3333/dates-available"
      {:params {:symbol symbol}
       :error-handler eb
       :handler (fn [dates]
                  (let [parsed-dates (reader/read-string dates)
                        ps (map #(get-price+ % symbol) parsed-dates)]
                    (-> (js/Promise.all ps)
                                                        ;; TODO - this needs to remove "USD" from string
                                                        ;; OR consider cutting the compilcation of USD on end
                        (.then #(apply min %))
                        (.then cb)
                        (.catch eb))))})
    (when name
      (http/GET "http://localhost:3333/symbol"
        {:params {:name name}
         :error-handler eb
         :handler (fn [symbol]
                    (http/GET "http://localhost:3333/dates-available"
                      {:params {:symbol symbol}
                       :error-handler eb
                       :handler (fn [dates]
                                  (let [parsed-dates (reader/read-string dates)
                                        ps (map #(get-price+ % symbol) parsed-dates)]
                                    (-> (js/Promise.all ps)
                                        (.then #(apply min %))
                                        (.then cb)
                                        (.catch eb))))}))}))))

(comment
  (get-low-price "Google" nil ok! fail!)
  (get-low-price nil "GOOGL" ok! fail!))

;;;;;;;;;;;;;;;;;;;;;;;;;

;;;;;;;;;;;;;;;;;;;

;; first, show the parts that are logic and parts that are effects
;; to show that it's not trivial to break it out
;; emphasize we can't trivially use our old trick

;; what about abstraction?
;; abstract into functions!
;; (do a checklist)
;; cool, we've improved duplication, code is shorter
;; have we made it more transparent? 
;; -- show that we haven't actually made our code more testable


(defn get-prices2 [symbol dates cb eb]
  (let [ps (map #(get-price+ % symbol) dates)]
    (-> (js/Promise.all ps)
        (.then #(apply min %))
        (.then cb)
        (.catch eb))))

(defn get-min-price-available [symbol cb eb]
  (http/GET "http://localhost:3333/dates-available"
    {:params {:symbol symbol}
     :error-handler eb
     :handler (fn [dates]
                (get-prices2 symbol (reader/read-string dates) cb eb))}))

(defn get-low-price2 [name symbol cb eb]
  (if symbol
    (get-min-price-available symbol cb eb)
    (when name
      (http/GET "http://localhost:3333/symbol"
        {:params {:name name}
         :error-handler eb
         :handler (fn [symbol]
                    (get-min-price-available symbol cb eb))}))))

(comment
  (get-low-price2 "Google" nil ok! fail!)
  (get-low-price2 nil "GOOGL" ok! fail!))

;;; what about promises??

;; TODO - maybe use get+ more places


(defn get+ [url params]
  (js/Promise. (fn [resolve reject]
                 (http/GET url
                   {:params params
                    :handler resolve
                    :error-handler reject}))))

(comment
  (-> (get+ "http://localhost:3333/dates-available" {:symbol "GOOGL"})
      (.then reader/read-string)
      (.then ok!)))

(defn get-prices2+ [symbol dates]
  (let [ps (map #(get-price+ % symbol) dates)]
    (-> (js/Promise.all ps)
        (.then #(apply min %)))))

(defn get-min-price-available+ [symbol]
  (-> (get+ "http://localhost:3333/dates-available" {:symbol symbol})
      (.then #(get-prices2+ symbol (reader/read-string %)))))

(defn get-low-price3 [name symbol cb eb]
  (-> (if symbol
        (get-min-price-available+ symbol)
        (when name
          (-> (get+ "http://localhost:3333/symbol" {:name name})
              (.then #(get-min-price-available+ %)))))
      (.then cb)
      (.catch eb)))

(comment
  (get-low-price3 "Google" nil ok! fail!)
  (get-low-price3 nil "GOOGL" ok! fail!))

;; what'd we get?
;; -- shorter code
;; -- less repetition of callbacks
;; -- less nesting
;; -- still no transparency
;; -- TODO - maybe eventually do version with promises and, say, circuit-breaker?

;; if not abstraction, or tools, what helps?

;; 1. (consider) avoiding decisions
;; 2. transparent functions
;; 3. co-locate IO
;; 4. Use tools to parallelize, improve readability
;; 5. (consider) using bigger requests (e.g. graphQL)


;;;;;;;;;;;;;;;;;;;;;;;;;

;; 1. avoid conditionals


(defn get-prices4 [symbol cb eb]
  (http/GET "http://localhost:3333/dates-available"
    {:params {:symbol symbol}
     :error-handler eb
     :handler (fn [dates]
                (let [parsed-dates (reader/read-string dates)
                      ps (map #(get-price+ % symbol) parsed-dates)]
                  (-> (js/Promise.all ps)
                      (.then #(apply min %))
                      (.then cb)
                      (.catch eb))))}))

(defn get-low-price4 [name symbol cb eb]
  (http/GET "http://localhost:3333/symbol"
    {:params {:name name}
     :error-handler #(get-prices4 symbol cb eb)
     :handler #(get-prices4 % cb eb)}))

(comment
  (get-low-price4 "Google" nil ok! fail!)
  (get-low-price4 nil "GOOGL" ok! fail!))

;; 2. extract pure functions


(defn price-reqs5 [dates-str symbol]
  (price-reqs (reader/read-string dates-str) symbol))

(defn dates-req [looked-up-symbol provided-symbol]
  {:url "http://localhost:3333/dates-available"
   :params {:symbol (or looked-up-symbol provided-symbol)}})

(defn symbol-req [name]
  {:url "http://localhost:3333/symbol"
   :params {:name name}})

(defn get!
  ([req cb]
   (get! req cb cb))
  ([req cb eb]
   (http/GET (:url req)
     (assoc req
            :handler cb
            :error-handler eb))))

(defn get+2 [req]
  (let [{:keys [url params]} req]
    (js/Promise. (fn [resolve reject]
                   (http/GET url
                     {:params params
                      :handler resolve
                      :error-handler reject})))))

(defn get-prices5 [looked-up-symbol provided-symbol cb eb]
  (let [req (dates-req looked-up-symbol provided-symbol)]
    (get! req
          (fn [dates]
            (let [reqs (price-reqs5 dates (-> req :params :symbol))
                  ps (map get+2 reqs)]
              (-> (js/Promise.all ps)
                  (.then #(apply min %))
                  (.then cb)
                  (.catch eb))))
          eb)))

(defn response->sym [response]
  (if (and (map? response) (= 404 (:status response)))
    nil
    response))

(defn get-low-price5 [name symbol cb eb]
  (get! (symbol-req name)
        #(get-prices5 (response->sym %) symbol cb eb)))

(comment
  (price-reqs5 "[\"2018-12-01\"]" "GOOGL")
  (get-low-price5 "Google" nil ok! fail!)
  (get-low-price5 nil "GOOGL" ok! fail!))


;;;;;;;;;;;;;;;;;;;;;;;;;;

;; co-locate IO


(defn price-reqs6 [dates-str symbol]
  (price-reqs (reader/read-string dates-str) symbol))

(defn dates-req6 [looked-up-symbol provided-symbol]
  {:url "http://localhost:3333/dates-available"
   :params {:symbol (or looked-up-symbol provided-symbol)}})

(defn symbol-req6 [name]
  {:url "http://localhost:3333/symbol"
   :params {:name name}})

(defn get!6
  ([req cb]
   (get! req cb cb))
  ([req cb eb]
   (http/GET (:url req)
     (assoc req
            :handler cb
            :error-handler eb))))

(defn get+6 [req]
  (let [{:keys [url params]} req]
    (js/Promise. (fn [resolve reject]
                   (http/GET url
                     {:params params
                      :handler resolve
                      :error-handler reject})))))

(defn response->sym6 [response]
  (if (and (map? response) (= 404 (:status response)))
    nil
    response))

(defn get-low-price6 [name symbol cb eb]
  (get! (symbol-req name)
        (fn [response]
          (let [looked-up-symbol (response->sym response)
                req (dates-req6 looked-up-symbol symbol)]
            (get! req
                  (fn [dates]
                    (let [reqs (price-reqs6 dates (-> req :params :symbol))
                          ps (map get+6 reqs)]
                      (-> (js/Promise.all ps)
                          (.then #(apply min %))
                          (.then cb)
                          (.catch eb))))
                  eb)))))

(comment
  (price-reqs6 "[\"2018-12-01\"]" "GOOGL")
  (get-low-price6 "Google" nil ok! fail!)
  (get-low-price6 nil "GOOGL" ok! fail!))

;; after relocating code, talk about benefits for error handling, performance
;; 
;; -- we actually have downsides (error handling, redundant effects, bigger API)


;; use tools to improve readability


(defn price-reqs7 [dates-str symbol]
  (price-reqs (reader/read-string dates-str) symbol))

(defn dates-req7 [looked-up-symbol provided-symbol]
  {:url "http://localhost:3333/dates-available"
   :params {:symbol (or looked-up-symbol provided-symbol)}})

(defn symbol-req7 [name]
  {:url "http://localhost:3333/symbol"
   :params {:name name}})

(defn get!7
  ([req cb]
   (get! req cb cb))
  ([req cb eb]
   (http/GET (:url req)
     (assoc req
            :handler cb
            :error-handler eb))))

(defn get+7 [req]
  (let [{:keys [url params]} req]
    (js/Promise. (fn [resolve reject]
                   (http/GET url
                     {:params params
                      :handler resolve
                      :error-handler reject})))))

;; TODO - rename
(defn get+7-both-paths [req]
  (let [{:keys [url params]} req]
    (js/Promise. (fn [resolve reject]
                   (http/GET url
                     {:params params
                      :handler resolve
                      :error-handler resolve})))))

(defn response->sym7 [response]
  (if (and (map? response) (= 404 (:status response)))
    nil
    response))

(defn get-low-price7 [name symbol cb eb]
  (-> (get+7-both-paths (symbol-req name))
      (.then (fn [response]
               (let [looked-up-symbol (response->sym response)
                     req (dates-req7 looked-up-symbol symbol)]
                 (js/Promise.all [(get+7 req) (js/Promise.resolve req)]))))
      (.then (fn [[dates req]]
               (let [reqs (price-reqs7 dates (-> req :params :symbol))
                     ps (map get+7 reqs)]
                 (js/Promise.all ps))))
      (.then #(apply min %))
      (.then cb)
      (.catch eb)))

(comment
  (get-low-price7 "Google" nil ok! fail!)
  (get-low-price7 nil "GOOGL" ok! fail!)
  (get-low-price7 nil nil ok! fail!))

;; finally, consider if builing bigger requests


(defn price-reqs8 [dates-str symbol]
  (price-reqs (reader/read-string dates-str) symbol))

(defn dates-req8 [looked-up-symbol provided-symbol]
  {:url "http://localhost:3333/dates-available"
   :params {:symbol (or looked-up-symbol provided-symbol)}})

(defn symbol-req8 [name]
  {:url "http://localhost:3333/symbol"
   :params {:name name}})

(defn get!8
  ([req cb]
   (get! req cb cb))
  ([req cb eb]
   (http/GET (:url req)
     (assoc req
            :handler cb
            :error-handler eb))))

(defn get+8 [req]
  (let [{:keys [url params]} req]
    (js/Promise. (fn [resolve reject]
                   (http/GET url
                     {:params params
                      :handler resolve
                      :error-handler reject})))))

;; TODO - rename
(defn get+8-both-paths [req]
  (let [{:keys [url params]} req]
    (js/Promise. (fn [resolve reject]
                   (http/GET url
                     {:params params
                      :handler resolve
                      :error-handler resolve})))))

(defn response->sym8 [response]
  (if (and (map? response) (= 404 (:status response)))
    nil
    response))

(defn post+ [req]
  (let [{:keys [url params format response-format]} req]
    (js/Promise. (fn [resolve reject]
                   (http/POST url
                     {:params params
                      :format format
                      :response-format response-format
                      :handler resolve
                      :error-handler resolve})))))

(defn form-data [m]
  (reduce
   (fn [fd [k v]]
     (.append fd (str k) v)
     fd)
   (js/FormData.)
   m))

(form-data {"a" 1 "b" 2})

(defn transit [x]
  (let [w (t/writer :json)]
    (t/write w x)))

(comment
  #_(http/POST "https://postman-echo.com/post"
      {:params {:message "Hello World"
                :user    "Bob"}
       :handler ok!
       :error-handler fail!})

  (http/GET "http://localhost:3333/symbolz"
    {:params {:symbol "GOOGL" :dates (pr-str ["2018-12-28", "2018-12-27"])}
     :response-format :text
     :handler ok!
     :error-handler fail!})

  (edn/edn-response-format)

  (-> (post+ {:url "http://localhost:3333/prices"
              :body (form-data {:symbol "GOOGL" :dates ["2018-12-27" "2018-12-28"]})
              :response-format (edn/edn-response-format)})

      (.then ok!)
      (.catch fail!))

  (http/POST
    "http://localhost:3333/prices"
    {:params {:symbol "GOOGL" :dates ["2018-12-28"]}
     :handler ok!
     :error-handler fail!})

  (http/POST
    "http://localhost:3333/prices"
    {:params {:symbol "GOOGL" :dates ["2018-12-28"]}
     :format :transit
     :handler ok!
     :error-handler fail!})

  (let [form-data (doto
                   (js/FormData.)
                    (.append "symbol" "GOOGL")
                    (.append "dates" ["2018-12-28"]))]
    (http/POST "http://localhost:3333/prices" {:body form-data
                                               :response-format (http/raw-response-format)
                                               :handler ok!
                                               :error-handler fail!})))

(defn prices-req [symbol dates]
  {:url "http://localhost:3333/symbolz"
   :params {:symbol symbol :dates (pr-str dates)}})

(defn get-low-price8 [name symbol cb eb]
  (-> (get+8-both-paths (symbol-req name))
      (.then (fn [response]
               (let [looked-up-symbol (response->sym response)
                     req (dates-req8 looked-up-symbol symbol)]
                 (js/Promise.all [(get+8 req) (js/Promise.resolve req)]))))
      (.then (fn [[dates-str req]]
               (prn [:bhb.dates dates-str])
               (get+8 (prices-req (-> req :params :symbol) (reader/read-string dates-str)))))
      (.then (fn [m]
               (->> (reader/read-string m)
                    vals
                    (map #(js/parseFloat (string/replace % " USD" "")))
                    (apply min))))
      (.then cb)
      (.catch eb)))

(comment
  (get-low-price8 "Google" nil ok! fail!)
  (get-low-price8 nil "GOOGL" ok! fail!)
  (get-low-price8 nil nil ok!



                  fail!))



;; 1. colocate as much as possible (reasoning) (Understanding comes from introspection and readability)
;; 2. use tools to improve readability
;; 3. consider avoiding decisions
;; 4. less chatty interface (graphQL)
;; 5.
