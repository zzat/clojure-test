(ns clojure-sandbox.4clojure)

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

;; Problem 34, Implement range
;; Write a function which creates a list of all integers in a given range.
(defn range-int
  [start end]
  (cond 
    (> start end) nil
    (= start end) []
    :else (cons start (range-int (inc start) end) )))

;; tests
(= [] (range-int 1 1))
(= [1 2] (range-int 1 3))
(= [1 2 3] (range-int 1 4))
(= nil (range-int 3 1))

;; Problem 38, Maximum value
;; Write a function which takes a variable number of parameters and returns the maximum value.

; (defn max-val
;   [ x & more ]
;   (cond 
;     (empty? more) x
;     :else (let [local-max (if (> x (first more)) x (first more))] 
;             (apply max-val local-max (rest more)))))

; (defn max-val
;   ([x] x)
;   ([x y] (if (> x y) x y))
;   ([ x y & more ] (apply max-val (max-val x y) more)))

(defn max-val
  [ & args ]
  (reduce #(if (> %1 %2) %1 %2) args))

;; tests
(= 1 (max-val 1))
(= 5 (max-val 1 5 3 4))

;; Problem 39, Interleave Two Seqs
;; Write a function which takes two sequences and returns the first item from each, then the second item from each, then the third, etc.

(defn interleave-seq
  [[elem1 & rest1 :as seq1] [elem2 & rest2 :as seq2]]
  (if (some empty? [seq1 seq2]) 
    [] 
    (into [elem1 elem2] (interleave-seq rest1 rest2))))

;; tests
(= [] (interleave-seq [1 2] []))
(= [1 1] (interleave-seq [1 2] [1]))
(= [1 1 2 3] (interleave-seq [1 2] [1 3]))

;; Problem 40, Interpose a Seq
;; Write a function which separates the items of a sequence by an arbitrary value.
(defn interpose-seq
  [sep coll]
  (reduce #(into %1 (if (empty? %1) [%2] [sep %2])) [] coll))

;; tests
(= [] (interpose-seq 0 []))
(= [1] (interpose-seq 0 [1]))
(= [1 0 2] (interpose-seq 0 [1 2]))
(= [1 0 2 0 3] (interpose-seq 0 '( 1 2 3)))

;; Problem 41, Drop Every Nth Item
;; Write a function which drops every Nth item from a sequence.

(defn drop-nth
  [coll n]
  (map :value 
   (filter #((comp not zero?) (mod (:index %) n))
           (map #(hash-map :index (inc %2) :value %1) coll (range)))))

  ;; tests
  (= [] (drop-nth [] 2))
  (= [] (drop-nth [1] 1))
  (= [1 2] (drop-nth [1 2 3] 3))
  (= [1 2 4 5 7] (drop-nth [1 2 3 4 5 6 7] 3))

;; Problem 42, Factorial Fun
;; Write a function which calculates factorials.

; (defn factorial 
;   [n]
;   (cond
;     (= n 1) 1 
;     :else (* n (factorial (dec n)))))

(defn factorial
  [n]
  (apply * (range 1 (inc n))))

;; tests
(= 1 (factorial 1))
(= 120 (factorial 5))

;; Problem 43, Reverse Interleave
;; Write a function which reverses the interleave process into x number of subsequences.

(defn reverse-interleave
  [coll n]
  (if (empty? coll) [] (apply map vector (partition n coll))))

;; tests
(= [] (reverse-interleave [] 2))
(= [[1] [2]] (reverse-interleave [1 2] 2))
(= [[1 4] [2 5] [3 6]] (reverse-interleave [1 2 3 4 5 6] 3))

;; Problem 44, Rotate Sequence
;; Write a function which can rotate a sequence in either direction.

(defn rotate-sequence
  [n coll]
  (cond
    (empty? coll) coll
    :else (let [rotate-by (mod n (count coll))
                [left right] (split-at rotate-by coll)]
            (concat right left))))

;; tests
(= [] (rotate-sequence 2 []))
(= [1 2 3] (rotate-sequence 0 [1 2 3]))
(= [3 1 2] (rotate-sequence 2 [1 2 3]))
(= [2 3 1] (rotate-sequence -2 [1 2 3]))
(= [2 3 1] (rotate-sequence 4 [1 2 3]))

;; Problem 46, Flipping out
;; Write a higher-order function which flips the order of the arguments of an input function.

(defn flip 
  [f]
  (fn [& args] (apply f (reverse args))))

;; tests
(= true ((flip >) 1 2))
(= [1 2] ((flip take) [1 2 3 4 5] 2))

;; Problem 49, Split a sequence
;; Write a function which will split a sequence into two parts.

(defn split-seq-at
  [n coll]
  [(take n coll) (drop n coll)])

;; tests
(= '(() ()) (split-seq-at 2 []))
(= '((1 2) (3 4 5)) (split-seq-at 2 [1 2 3 4 5]))

;; Problem 50, Split by Type
;; Write a function which takes a sequence consisting of items with different types and splits them up into a set of homogeneous sub-sequences. The internal order of each sub-sequence should be maintained, but the sub-sequences themselves can be returned in any order (this is why 'set' is used in the test cases).

(defn split-by-type
  [coll]
  (let [update-fn (fn [old-val new-val]
                    (if (nil? old-val) 
                      [new-val] 
                      (conj old-val new-val)))]
    (loop [type-map {}
           [elem & more :as remaining] coll]
    (if (empty? remaining) 
      (vals type-map) 
      (recur (update type-map (type elem) update-fn elem) more)))))

;; tests
(= [] (split-by-type []))
(= [[1]] (split-by-type [1]))
(= [[1] [:a]] (split-by-type [1 :a]))
(= [[1 2] [:a :b] ["ABC"]] (split-by-type [1 :a :b 2 "ABC"]))

;; Problem 53, Longest Increasing Sub-Seq
;; Given a vector of integers, find the longest consecutive sub-sequence of increasing numbers. If two sub-sequences have the same length, use the one that occurs first. An increasing sub-sequence must have a length of 2 or greater to qualify.

(defn longest-increasing-sub-seq
  [coll]
  (loop [longest-seq []
         current-seq []
         last-element nil
         [ elem & more :as remaining ] coll]
    (cond 
      (empty? remaining) (let [longest-seq-length (count longest-seq)
                               current-seq-length (count current-seq)]
                           (if (and (< longest-seq-length 2) (< current-seq-length 2))
                           []
                           (if (> current-seq-length longest-seq-length) current-seq longest-seq)))
      :else (if (or (nil? last-element) (> elem last-element))
              (recur longest-seq (conj current-seq elem) elem more)
              (if (> (count current-seq) (count longest-seq)) 
                (recur current-seq [elem] elem more)
                (recur longest-seq [elem] elem more))))))

;; tests
(= [] (longest-increasing-sub-seq [1]))
(= [] (longest-increasing-sub-seq [6 5 3 2]))
(= [5 7 9] (longest-increasing-sub-seq [6 5 7 9]))

;; Problem 54, Partition a Sequence
;; Write a function which returns a sequence of lists of x items each. Lists of less than x items should not be returned.
(defn partition-seq
  [n coll]
  (cond 
    (empty? coll) []
    :else (let [first-part (take n coll)
                remaining-part (drop n coll)]
            (if (< (count first-part) n)
              []
              (cons first-part (partition-seq n remaining-part))))))

;; tests
(= (partition-seq 3 (range 9)) '((0 1 2) (3 4 5) (6 7 8)))
(= (partition-seq 2 (range 8)) '((0 1) (2 3) (4 5) (6 7)))
(= (partition-seq 3 (range 8)) '((0 1 2) (3 4 5)))

;; Problem 55, Count Occurences
;; Write a function which returns a map containing the number of occurences of each distinct item in a sequence. 

(defn count-occurences
  [coll]
  (let [update-fn (fn [old-val new-val] (if (nil? old-val) 1 (inc old-val)))]
    (loop [frequency-map {}
           [elem & remaining :as elems] coll]
      (cond 
        (empty? elems) frequency-map
        :else (recur (update frequency-map elem update-fn 1) remaining)))))

;; tests
(= (count-occurences [1 1 2 3 2 1 1]) {1 4, 2 2, 3 1})
(= (count-occurences [:b :a :b :a :b]) {:a 2, :b 3})
(= (count-occurences '([1 2] [1 3] [1 3])) {[1 2] 1, [1 3] 2})

;; Problem 56, Find Distinct Items
;; Write a function which removes the duplicates from a sequence. Order of the items must be maintained.

(defn distinct-items
  [coll]
  (reduce #(if (contains? (set %1) %2) %1 (conj %1 %2)) [] coll))

;; tests
(= (distinct-items [1 2 1 3 1 2 4]) [1 2 3 4])
(= (distinct-items [:a :a :b :b :c :c]) [:a :b :c])
(= (distinct-items '([2 4] [1 2] [1 3] [1 3])) '([2 4] [1 2] [1 3]))
(= (distinct-items (range 50)) (range 50))

;; Problem 58, Function Composition
;; Write a function which allows you to create function compositions. The parameter list should take a variable number of functions, and create a function applies them from right-to-left.

(defn compose
  [& functions]
  (cond 
    (empty? functions) nil
    :else (fn [& args]
            ((fn fn-comp [[first-fn & rest-fn]]
              (if (nil? rest-fn) 
                (apply first-fn args) 
                (first-fn (fn-comp rest-fn)))) functions))))

; rest (fn-comp reverse)
; (fn x (reverse x))

;; tests
(= [3 2 1] ((fn-comp rest reverse) [1 2 3 4]))
(= 5 ((fn-comp (partial + 3) second) [1 2 3 4]))
(= true ((fn-comp zero? #(mod % 8) +) 3 5 7 9))
(= "HELLO" ((fn-comp #(.toUpperCase %) #(apply str %) take) 5 "hello world"))

;; Problem 59, Juxtaposition
;; Take a set of functions and return a new function that takes a variable number of arguments and returns a sequence containing the result of applying each function left-to-right to the argument list.

(defn juxtaposition
  [ & functions ]
  (fn [ & args ]
    (map #(apply % args) functions)))

;; tests
(= [21 6 1] ((juxtaposition + max min) 2 3 5 1 6 4))
(= ["HELLO" 5] ((juxtaposition #(.toUpperCase %) count) "hello"))
(= [2 6 4] ((juxtaposition :a :c :b) {:a 2, :b 4, :c 6, :d 8 :e 10}))

;; Problem 60, Sequence Reductions
;; Write a function which behaves like reduce, but returns each intermediate value of the reduction. Your function must accept either two or three arguments, and the return sequence must be lazy.

(defn seq-reduction
  ([func acc coll]
   (cond
    (empty? coll) [acc]
    :else (lazy-seq (let [step-result (func acc (first coll))]
            (cons acc (seq-reduction func step-result (rest coll)))))))
  ([func coll] (seq-reduction func (first coll) (rest coll))))

;; tests
(= (take 5 (seq-reduction + (range))) [0 1 3 6 10])
(= (seq-reduction conj [1] [2 3 4]) [[1] [1 2] [1 2 3] [1 2 3 4]])
(= (last (seq-reduction * 2 [3 4 5])) (reduce * 2 [3 4 5]) 120)

;; Problem 61, Map Construction
;; Write a function which takes a vector of keys and a vector of values and constructs a map from them.

(defn construct-map
  [ks vs]
  (into {} (map #(hash-map %1 %2) ks vs)))

;; tests
(= (construct-map [:a :b :c] [1 2 3]) {:a 1, :b 2, :c 3})
(= (construct-map [1 2 3 4] ["one" "two" "three"]) {1 "one", 2 "two", 3 "three"})
(= (construct-map [:foo :bar] ["foo" "bar" "baz"]) {:foo "foo", :bar "bar"})

;; Problem 62, Re-implement Iteration
;; Given a side-effect free function f and an initial value x write a function which returns an infinite lazy sequence of x, (f x), (f (f x)), (f (f (f x))), etc.

(defn iterate*
  [f x]
  (lazy-seq (cons x (iterate* f (f x)))))

;;tests
(= (take 5 (iterate* #(* 2 %) 1)) [1 2 4 8 16])
(= (take 100 (iterate* inc 0)) (take 100 (range)))
(= (take 9 (iterate* #(inc (mod % 3)) 1)) (take 9 (cycle [1 2 3])))

;; Problem 63, Group a Sequence
;; Given a function f and a sequence s, write a function which returns a map. The keys should be the values of f applied to each item in s. The value at each key should be a vector of corresponding items in the order they appear in s.

(defn group-seq
  [f s]
  (let [update-fn (fn [old-val new-val] 
                    (if (nil? old-val) 
                      [new-val] 
                      (conj old-val new-val)))]
    (loop [grouped-map {}
           coll s]
      (cond
        (empty? coll) grouped-map
        :else (recur (update grouped-map (f (first coll)) update-fn (first coll)) (rest coll))))))

;; tests
(= (group-seq #(> % 5) #{1 3 6 8}) {false [1 3], true [6 8]})
(= (group-seq #(apply / %) [[1 2] [2 4] [4 6] [3 6]])
   {1/2 [[1 2] [2 4] [3 6]], 2/3 [[4 6]]})
(= (group-seq count [[1] [1 2] [3] [1 2 3] [2 3]])
   {1 [[1] [3]], 2 [[1 2] [2 3]], 3 [[1 2 3]]}) 

;; Problem 66, Greatest Common Divisor
;; Given two integers, write a function which returns the greatest common divisor.

(defn gcd
  [i1 i2]
  (cond
    (= i1 i2) i1
    (> i1 i2) (gcd (- i1 i2) i2)
    :else (gcd i1 (- i2 i1))))

;; tests
(= (gcd 2 4) 2)
(= (gcd 10 5) 5)
(= (gcd 5 7) 1)
(= (gcd 1023 858) 33)

;; Problem 67, Prime Numbers
;; Write a function which returns the first x number of prime numbers.

(defn n-primes
  [n]
  (let [isPrime (fn [x] 
                  (cond
                    (= 2 x) true
                    :else (not (some #(zero? (mod x %)) (range 2 (inc (Math/sqrt x)))))))] 
    (take n (filter isPrime (map (partial + 2) (range))))))

;; tests
(= (n-primes 2) [2 3])
(= (n-primes 5) [2 3 5 7 11])
(= (last (n-primes 100)) 541)

;; Problem 70, Word Sorting
;; Write a function which splits a sentence up into a sorted list of words. Capitalization should not affect sort order and punctuation should be ignored.

(defn sort-words
  [sentence]
  (sort-by #(clojure.string/lower-case %) (re-seq #"\w+" sentence)))

;; tests
(= (sort-words  "Have a nice day.")
   ["a" "day" "Have" "nice"])
(= (sort-words  "Clojure is a fun language!")
   ["a" "Clojure" "fun" "is" "language"])
(= (sort-words  "Fools fall for foolish follies.")
   ["fall" "follies" "foolish" "Fools" "for"])

;; Problem 73, Analyze a Tic-Tac-Toe Board
;; A tic-tac-toe board is represented by a two dimensional vector. X is represented by :x, O is represented by :o, and empty is represented by :e. A player wins by placing three Xs or three Os in a horizontal, vertical, or diagonal row. Write a function which analyzes a tic-tac-toe board and returns :x if X has won, :o if O has won, and nil if neither player has won.

(defn analyze-tic-tac-toe
  [board]
  (let [rows board
        cols (apply (partial map #(list %1 %2 %3)) rows)
        diags [[((board 0) 0) ((board 1) 1) ((board 2) 2)] 
               [((board 0) 2) ((board 1) 1) ((board 2) 0)]]
        check (fn [k]
                (some (fn [vals] (every? #(= k %) vals)) (into (into rows cols) diags)))]
    (cond 
      (check :x) :x
      (check :o) :o 
      :else nil)
    ))

;; tests
(= nil (analyze-tic-tac-toe [[:e :e :e]
            [:e :e :e]
            [:e :e :e]]))
(= :x (analyze-tic-tac-toe [[:x :e :o]
           [:x :e :e]
           [:x :e :o]]))
(= :o (analyze-tic-tac-toe [[:e :x :e]
           [:o :o :o]
           [:x :e :x]]))
(= nil (analyze-tic-tac-toe [[:x :e :o]
            [:x :x :e]
            [:o :x :o]]))
(= :x (analyze-tic-tac-toe [[:x :e :e]
           [:o :x :e]
           [:o :e :x]]))
(= :o (analyze-tic-tac-toe [[:x :e :o]
           [:x :o :e]
           [:o :e :x]]))
(= nil (analyze-tic-tac-toe [[:x :o :x]
            [:x :o :x]
            [:o :x :o]]))
