(ns clojure_sandbox.4clojure)

;; Problem 19, Last Element
;; Write a function which returns the last element in a sequence.

(defn last-element
  [[elem & remaining]] 
  (if (nil? remaining) elem (last-element remaining)))

;; tests
(= nil (last-element []))
(= 1 (last-element [1]))
(= 2 (last-element [1 2]))
(= 4 (last-element [1 2 3 4]))

;; Problem 20, Penultimate Element
;; Write a function which returns the second to last element from a sequence.

(defn second-last-element
  [[elem1 elem2 & remaining]]
  (if (nil? remaining) 
    (if ( nil? elem2) nil elem1) 
    (second-last-element (conj remaining elem2))))

;; tests
(= nil (second-last-element []))
(= nil (second-last-element [1]))
(= 1 (second-last-element [1 2]))
(= 3 (second-last-element [1 2 3 4]))
(= [1 2] (second-last-element [[ 1 2] [ 3 4]]))

;; Problem 21, Nth Element
;; Write a function which returns the Nth element from a sequence.

(defn nth-element
  [[elem & remaining] n]
  (if (= 0 n)
    elem
    (if (or (< n 0) (nil? remaining))
      nil
      (nth-element remaining (dec n)))))

;; tests
(= nil (nth-element [] 1))
(= nil (nth-element [] 5))
(= 1 (nth-element [1] 0))
(= 2 (nth-element [nil 2 3] 1))
(= 2 (nth-element '(1 2 3) 1))
(= 3 (nth-element [1 2 3] 2))
(= nil (nth-element [1 2 3] 3))
(= nil (nth-element [1 2 3] -1))

;; Problem 22, Count a Sequence
;; Write a function which returns the total number of elements in a sequence.

(defn count-seq
  [xs]
  (if (empty? xs)
    0
    (inc (count-seq (rest xs)))))

;;tests
(= 0 (count-seq []))
(= 1 (count-seq [1]))
(= 4 (count-seq [1 nil 3 4]))
(= 2 (count-seq '( 1 2)))
(= 3 (count-seq #{ 1 2 3}))
(= 1 (count-seq {:a 1}))

;; Problem 23, Reverse a Sequence
;; Write a function which reverses a sequence.

(defn reverse-seq
  [xs]
  (loop [acc '() 
         remaining xs]
    (if (seq remaining)
      (recur (cons (first remaining) acc) (rest remaining))
      acc)))

;; tests
(= [] (reverse-seq []))
(= [1] (reverse-seq [1]))
(= [2 1] (reverse-seq [1 2]))
(= [4 3 nil 1] (reverse-seq [1 nil 3 4]))
(= '(4 3 2 1) (reverse-seq '(1 2 3 4)))

;; Problem 24, Sum It All Up
;; Write a function which returns the sum of a sequence of numbers.

(defn sum
  [xs]
  (if (empty? xs)
    0
    (+ (first xs) (sum (rest xs)))))

;; tests
(= 0 (sum []))
(= 1 (sum [1]))
(= 3 (sum [1 2]))
(= 6 (sum #{1 2 3}))

;; Problem 25, Find the odd numbers
;; Write a function which returns only the odd numbers from a sequence.

(defn filter-odd
  [xs]
  (filter #(not= 0 (mod % 2)) xs))

;; tests
(= [] (filter-odd []))
(= [1] (filter-odd [1]))
(= [] (filter-odd [2]))
(= [1] (filter-odd [1 2]))
(= [1 3 5] (filter-odd [1 2 3 4 5]))
(= [1 3 5] (filter-odd [1 2 3 4 5 6]))

;; Problem 26, Fibonacci Sequence
;; Write a function which returns the first X fibonacci numbers

(defn fibonacci
  [n]
  (cond 
    (< n 1) nil
    (= 1 n) [1]
    (= 2 n) [1 1] 
    :else 
      (loop [result [1 1]
             count (- n 2)]
        (if (pos? count)
          (let [next-elem (sum (take-last 2 result))] 
            (recur (conj result next-elem) (dec count)))
          result))))

;; tests
(= nil (fibonacci -1))
(= nil (fibonacci 0))
(= [1] (fibonacci 1))
(= [1 1] (fibonacci 2))
(= [1 1 2] (fibonacci 3))
(= [1 1 2 3] (fibonacci 4))

;; Problem 27, Palindrome Detector
;; Write a function which returns true if the given sequence is a palindrome. 
;; Hint: "racecar" does not equal '(\r \a \c \e \c \a \r)

(defn palindrome
  [xs]
  (= (seq xs) (reverse xs)))

;; tests
(= true (palindrome "a"))
(= true (palindrome "aba"))
(= false (palindrome "abc"))
(= true (palindrome "abba"))

;; Problem 28, Flatten a Sequence
;; Write a function which flattens a sequence.

(defn flatten-seq
  [xs]
  (cond
    (empty? xs) []
    :else (if (sequential? (first xs))
            (concat (flatten-seq (first xs)) (flatten-seq (rest xs)))
            (cons (first xs) (flatten-seq (rest xs))))))

; (defn flatten-seq
;   [xs]
;   (loop [acc []
;          remaining xs]
;     (if (empty? remaining)
;       acc
;       (let [x (first remaining)]
;         (if (sequential? x)
;           (recur (concat acc (flatten-seq x)) (rest remaining))
;           (recur (conj acc x) (rest remaining)))))))

;; tests
(= [] (flatten-seq []))
(= [1] (flatten-seq [1]))
(= [1 2 3] (flatten-seq [1 2 3]))
(= [1 nil 3] (flatten-seq [1 nil 3]))
(= [1 2 3] (flatten-seq [1 [2 3]]))
(= [1 2 3 4 5 6] (flatten-seq [1 [2 3 [4 [5 6]]]]))
(= '(1 2 3) (flatten-seq '(1 (2 3))))

;; Problem 29, Get the Caps
;; Write a function which takes a string and returns a new string containing only the capital letters.

(defn get-caps
  [string]
  (clojure.string/join (filter #(Character/isUpperCase %) string)))

;; tests
(= "" (get-caps ""))
(= "" (get-caps "a"))
(= "" (get-caps "abcd"))
(= "A" (get-caps "Abcd"))
(= "AC" (get-caps "AbCd"))
(= "AC" (get-caps "#A$fCd"))

;; Problem 30, Compress a Sequence
;; Write a function which removes consecutive duplicates from a sequence.

(defn remove-consecutive-duplicates
  [xs]
  (cond
    (empty? xs) xs
    :else (cons 
            (first xs) 
            (remove-consecutive-duplicates 
              (drop-while #(= (first xs) %) (rest xs))))))

; try with reduce too

;; tests
(= [] (remove-consecutive-duplicates []))
(= [1] (remove-consecutive-duplicates [1]))
(= [1 2] (remove-consecutive-duplicates [1 2 2]))
(= [1 2 3] (remove-consecutive-duplicates [1 2 2 3]))
(= [1 2 3 4 5] (remove-consecutive-duplicates [1 2 2 3 4 5 5]))

;; Problem 31, Pack a Sequence
;; Write a function which packs consecutive duplicates into sub-lists

(defn pack-sequence
  [xs]
  (cond
    (empty? xs) xs
    :else (let [[pack-of-first remaining] (split-with #(= % (first xs)) xs)]
            (cons pack-of-first (pack-sequence remaining)))))

;; tests
(= [] (pack-sequence []))
(= ['(1)] (pack-sequence [1]))
(= ['(1) '(2)] (pack-sequence [1 2]))
(= ['(1) '(2 2)] (pack-sequence [1 2 2]))
(= ['(1) '(2 2) '(3) '(4) '(5 5)] (pack-sequence [1 2 2 3 4 5 5]))

;; Problem 32, Duplicate a Sequence
;; Write a function which duplicates each element of a sequence.

(defn duplicate-sequence
  [xs]
  (cond
    (empty? xs) xs
    :else (cons (first xs) (cons (first xs) (duplicate-sequence (rest xs))))))

;; tests
(= [] (duplicate-sequence []))
(= [1 1] (duplicate-sequence [1]))
(= [1 1 1 1] (duplicate-sequence [1 1]))
(= [1 1 1 1 2 2] (duplicate-sequence [1 1 2]))

;; Problem 33, Replicate a Sequence
;; Write a function which replicates each element of a sequence a variable number of times.

(defn replicate-sequence
  [xs n]
  (cond 
    (empty? xs) xs
    (< n 1) []
    :else (concat 
            (repeat n (first xs)) 
            (replicate-sequence (rest xs) n))))

; without repeat
; (defn replicate-sequence
;   [xs n]
;   (cond
;     (empty? xs) xs
;     (< n 1) []
;     :else ((fn cons-n [iteration] 
;             (if (pos? iteration) 
;               (cons (first xs) (cons-n (dec iteration)))
;               (replicate-sequence (rest xs) n)))
;            n)))

;; tests
(= [] (replicate-sequence [] 10))
(= [] (replicate-sequence [1] 0))
(= [1 2] (replicate-sequence [1 2] 1))
(= [1 1 1 2 2 2 3 3 3] (replicate-sequence [1 2 3] 3))
