(ns cljs.client1
  (:require
   [devtools.core :as devtools]
   [ajax.core :as http]
   [clojure.string :as string]
   [cljs.reader :as reader]
   [cognitect.transit :as t]))

(devtools/install!)

;; Pure functions

(defn foo [key-fn]
  (juxt key-fn identity))

;; Easy to test? Yes
;; Easy to iterate upon? No
;; Easy to reason about? No

(foo first) ; #object[G__12349]
(foo :a)    ; #object[G__12349]

((foo first) [1 2]) ; [1 [1 2]]
((foo :a) {:a 1})   ; [1 {:a 1}]

;; Pure is good, but *transparent* is even better

(defn baz [m1 m2]
  (-> m1
      (into m2)
      (assoc :total-keys (+ (count m1)
                            (count m2)))))

(baz {} {})          ; {:total-keys 0}
(baz {:x 1} {:y 2})  ; {:x 1, :y 2, :total-keys 2}
