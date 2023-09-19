(ns clojure-sandbox.brave_clojure_exercises)

; Chapter: Do Things

; 1. Use the str, vector, list, hash-map, and hash-set functions.
(defn hello
  "Greeter function"
  [name]
  (str "Hello " name "!"))

(hello "Bob")

(= (vector 1 2 3 4) [1 2 3 4])

(= (list 5 6 7 8) '(5 6 7 8))

(= (hash-map :a 1 :b 2) {:a 1 :b 2})

(= (hash-set 1 2 3 4) #{1 2 3 4})

; 2. Write a function that takes a number and adds 100 to it
(defn inc-maker
  "Creates a function that adds a fixed value to its argument"
  [n]
  (fn [x] (+ x n)))

(def add-100 (inc-maker 100))
(add-100 1)

; 3. Write a function, dec-maker, that works exactly like the function inc-maker except with subtraction
(def dec-maker 
  (fn 
    [x] 
    (inc-maker (* -1 x))))

(def dec-9 (dec-maker 9))
(dec-9 20)
