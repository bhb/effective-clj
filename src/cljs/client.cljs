(ns client)

(js/alert "hi")

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
      (assoc :count (+ (count m1)
                       (count m2)))))

(baz {} {})

;; Example - getting stock price via symbol or ticker









