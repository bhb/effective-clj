(ns cljs.client
  (:require
   [devtools.core :as devtools]
   [ajax.core :as http]
   [clojure.string :as string]
   [cljs.reader :as reader]))

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
      (assoc :total-keys (+ (count m1)
                            (count m2)))))

(baz {} {})
(baz {:x 1} {:y 2})

(defn handler [response]
  (prn response))

(defn error-handler [{:keys [status status-text]}]
  (prn (str "something bad happened: " status " " status-text)))

(comment
  (http/GET "http://localhost:3333/symbol" {:handler handler
                                            :error-handler error-handler
                                            :params {:name "google"}}))

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


(defn today []
  (let [t (js/Date.)
        dd (.getDate t)
        dd (if (< dd 10) (str "0" dd) dd)
        mm (inc (.getMonth t))
        mm (if (< mm 10) (str "0" mm) mm)
        yyyy (.getFullYear t)]
    (str yyyy "-" mm "-" dd)))

(defn get-price [date symbol cb eb]
  (if symbol
    (http/GET "http://localhost:3333/price"
      {:handler (fn [response]
                  (cb
                   (-> response
                       (string/replace " USD" "")
                       js/parseFloat)))
       :error-handler (fn [response] (eb response))
       :params {:symbol (-> symbol string/upper-case string/trim)
                :date (-> (or date (today)) string/trim (string/replace #"/" "-"))}})
    (cb nil)))

(comment
  (get-price "2018-12-28" "GOOGL" (fn [x] (prn [:done x])) (fn [x] (prn [:err x]))))

(defn price-req [date today symbol]
  (if symbol
    {:action :get
     :url "http://localhost:3333/price"
     :handler (fn [response]
                (-> response
                    (string/replace " USD" "")
                    js/parseFloat))
     :error-handler (fn [response] response)
     :params {:symbol (-> symbol string/upper-case string/trim)
              :date (-> (or date today) string/trim (string/replace #"/" "-"))}}
    {:action :noop}))

(defn ok! [x]
  (prn [:ok x]))

(defn fail! [x]
  (prn [:fail x]))

(comment
  (price-req nil "2018-12-29" nil)
  (price-req nil "2018-12-29" "googl")
  (price-req "2018-12-28" "2018-12-29" "GOOGL")

  (-> (price-req "2018-12-28" "2018-12-29" "GOOGL")
      (update :handler #(comp ok! %))
      :handler
      (apply ["200.1 USD"]))

  (let [response "200.1 USD"]
    (-> response
        (string/replace " USD" "")
        js/parseFloat)))

(defn get-price2 [date symbol cb eb]
  (let [req (price-req date (today) symbol)]
    (case (:action req)
      :get (let [req' (-> req
                          (update :handler #(comp cb %))
                          (update :error-handler #(comp eb %)))]
             (http/GET (:url req') req'))
      :noop (cb nil))))

(comment
  (get-price2 "2018-12-28" "GOOGL" ok! fail!)
  (get-price2 "2018/12/28" "googl" ok! fail!))

;;;;;;;;;;;;; independent ;;;;;;;;;;;;;;;;;;;;


(defn mean [prices]
  (/ (apply + prices)
     (count prices)))

(defn get-prices [dates symbol prices cb eb]
  (if (empty? dates)
    (cb prices)
    (let [[first-date & rest] dates]
      (get-price first-date
                 symbol
                 (fn [price]
                   (get-prices
                    rest
                    symbol
                    (conj prices price)
                    cb
                    eb))
                 eb))))

(comment
  (get-prices ["2018-12-26" "2018-12-27" "2018-12-28"] "GOOGL" [] (fn [x] (prn [:done x]))))

;; First attempt: recursion


(defn get-mean-price1 [dates symbol cb eb]
  (get-prices dates
              symbol
              []
              (fn [prices]
                (cb (mean prices)))
              eb))

(comment
  (get-mean-price1 ["2018-12-26" "2018-12-27" "2018-12-28"] "GOOGL" (fn [x] (prn [:done x])) (fn [x] (prn [:err x]))))

;; TODO - maybe write get+ instead here
(defn get-price+ [date symbol]
  (js/Promise. (fn [resolve reject]
                 (get-price
                  date
                  symbol
                  resolve
                  reject))))

(comment
  (-> (get-price+ "2017-12-26" "GOOGL")
      (.then (fn [x] (prn [:done x])))))

;; Step 1, break into independent effects

(defn get-mean-price2 [dates symbol cb eb]
  (let [ps (map #(get-price+ % symbol) dates)]
    (-> (js/Promise.all ps)
        (.then mean)
        (.then cb)
        (.catch eb))))

(comment
  (get-mean-price2 ["2018-12-26" "2018-12-27" "2018-12-28"] "GOOGL" (fn [x] (prn [:done x])) (fn [x] (prn [:err x]))))

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
  (get-mean-price3 ["2018-12-26" "2018-12-27" "2018-12-28"] "GOOGL" (fn [x] (prn [:done x])) (fn [x] (prn [:err x]))))

(comment
  (let [p (js/Promise. (fn [resolve reject] (js/setTimeout (fn [] (resolve "foo")) 1000)))]
    (-> p
        (.then (fn [x] (prn x)))))

  (let [p (js/Promise. (fn [resolve reject]
                         (let [req (price-req nil (today) "googl" resolve reject)]
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
                                                        (.then #(apply min %))
                                                        (.then cb)
                                                        (.catch eb))))})
    (if name
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
  (get-low-price nil "GOOGL" ok! fail!)
  )

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
    (if name
      (http/GET "http://localhost:3333/symbol"
                {:params {:name name}
                 :error-handler eb
                 :handler (fn [symbol]
                            (get-min-price-available symbol cb eb))}))))

(comment
  (get-low-price2 "Google" nil ok! fail!)
  (get-low-price2 nil "GOOGL" ok! fail!)
  )

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
      (.then ok!)
      ))


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
        (if name
          (-> (get+ "http://localhost:3333/symbol" {:name name})
              (.then #(get-min-price-available+ %)))))
      (.then cb)
      (.catch eb)))

(comment
  (get-low-price3 "Google" nil ok! fail!)
  (get-low-price3 nil "GOOGL" ok! fail!)
  )

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
;; 4. (consider) using bigger requests (e.g. graphQL)
;; 5. Use tools to parallelize, improve readability

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
  (get-low-price4 nil "GOOGL" ok! fail!)
  )

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
  ;; TODO: get! could take one arg which means use for both
  (get! (symbol-req name)
        #(get-prices5 (response->sym %) symbol cb eb)))

(comment
  (price-reqs5 "[\"2018-12-01\"]" "GOOGL")
  (get-low-price5 "Google" nil ok! fail!)
  (get-low-price5 nil "GOOGL" ok! fail!)
  )


;;;;;;;;;;;;;;;;;;;;;;;;;;


;; after relocating code, talk about benefits for error handling, performance
;; 
;; -- we actually have downsides (error handling, redundant effects, bigger API)


;; 1. colocate as much as possible (reasoning) (Understanding comes from introspection and readability)
;; 2. use tools to improve readability
;; 3. consider avoiding decisions
;; 4. less chatty interface (graphQL)
;; 5.
