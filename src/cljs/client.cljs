(ns cljs.client
  (:require
   [devtools.core :as devtools]
   [ajax.core :as http]
   [clojure.string :as string]))

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

(defn get+ [url params]
  (js/Promise. (fn [resolve reject]
                 (http/GET url
                           {:params params
                    :handler resolve
                    :error-handler reject}))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;;; TODO remove this section ;;;;;;;;

(defn get-price! [date symbol cb eb]
  (if symbol
    (http/GET "http://localhost:3333/price"
              {:handler cb
               :error-handler eb
               :params {:symbol (-> symbol string/upper-case string/trim)
                        :date (-> (or date (get-today!)) string/trim (string/replace #"/" "-"))}})
    (cb nil)))

;; TODO - maybe write get+ instead here
(defn get-price+ [date symbol]
  (js/Promise. (fn [resolve reject]
                 (get-price!
                  date
                  symbol
                  resolve
                  reject))))

(defn price-req [date today symbol]
  (if symbol
    {:action :get
     :url "http://localhost:3333/price"
     ;; TODO - don't put handler or error-handler here
     :params {:symbol (-> symbol string/upper-case string/trim)
              :date (-> (or date today) string/trim (string/replace #"/" "-"))}}
    {:action :noop}))

(defn price-reqs [dates symbol]
  (->> dates
       (map #(price-req % nil symbol))
       (remove #(= :noop (:action %)))))

;;;;;;;;;;;;;;;;;;;;;;;;;;
