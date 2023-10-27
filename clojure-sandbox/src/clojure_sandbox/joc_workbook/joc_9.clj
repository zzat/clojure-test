(ns clojure-sandbox.joc-workbook.joc-9
  (:refer-clojure :exclude [defstruct get cat])
  (:use (clojure set xml))
  (:use [clojure.test :only (are is)])
  (:require (clojure [zip :as z]))
  (:import (java.util Date)
           (java.io File)))

(defn beget [this proto]
  (assoc this ::prototype proto))

(defn get [m k]
  (when m
    (if-let [[_ v] (find m k)]
      v
      (recur (::prototype m) k))))

(def put assoc)

(def cat {:likes-dogs true, :ocd-bathing true})
(def morris (beget {:likes-9lives true} cat))
(def post-traumatic-morris (beget {:likes-dogs nil} morris))
(get cat :likes-dogs)
(get morris :likes-dogs)
(get post-traumatic-morris :likes-dogs)

(defmulti compiler :os)
(defmethod compiler ::unix [m] (get m :c-compiler))
(defmethod compiler ::osx [m] (get m :llvm-compiler))

(def clone (partial beget {}))
(def unix {:os ::unix, :c-compiler "cc", :home "/home", :dev "/dev"})
(def osx (-> (clone unix)
             (put :os ::osx)
             (put :llvm-compiler "clang")
             (put :home "/Users")))

(compiler unix)
(compiler osx)

(defmulti home :os)
(defmethod home ::unix [m] (get m :home))
(home unix)
(home osx)

(derive ::osx ::unix)
(home osx)

;; Query hierarchy
(parents ::osx)
;=> #{:user/unix}
(ancestors ::osx)
;=> #{:user/unix}
(descendants ::unix)
;=> #{:user/osx}
(isa? ::osx ::unix)
;=> true
(isa? ::unix ::osx)
;=> false

(derive ::osx ::bsd)
(defmethod home ::bsd [m] "/home")
(home osx) ;; Multiple methods in multimethod error

(prefer-method home ::unix ::bsd)
(home osx)

(derive (make-hierarchy) ::osx ::unix)

(defmulti compile-cmd (juxt :os compiler))
(defmethod compile-cmd [::osx "gcc"] [m] (str "/usr/bin" (get m :c-compiler)))
(defmethod compile-cmd :default [m] (str "Unsure where to locate " (get m :c-compiler)))

(compile-cmd osx)
(compile-cmd unix)

(defrecord TreeNode [val l r])
(TreeNode. 5 nil nil)

(defn xconj [t v] (cond
                    (nil? t)       (TreeNode. v nil nil)
                    (< v (:val t)) (TreeNode. (:val t) (xconj (:l t) v) (:r t))
                    :else          (TreeNode. (:val t) (:l t) (xconj (:r t) v))))

(defn xseq [t]
  (when t
    (concat (xseq (:l t)) [(:val t)] (xseq (:r t)))))

(def sample-tree (reduce xconj nil [3 5 2 4 6]))
(xseq sample-tree)

(defprotocol FIXO
  (fixo-push [fixo value])
  (fixo-pop [fixo])
  (fixo-peek [fixo]))

(extend-type TreeNode
  FIXO
  (fixo-push [node value]
    (xconj node value)))

(xseq (fixo-push sample-tree 5/2))

(extend-type clojure.lang.IPersistentVector
  FIXO
  (fixo-push [vector value]
    (conj vector value)))

(fixo-push [2 3 4 5 6] 5/2)

(reduce fixo-push nil [3 5 2 4 6 0])

(extend-type nil
  FIXO
  (fixo-push [t v]
    (TreeNode. v nil nil)))

(xseq (reduce fixo-push nil [3 5 2 4 6 0]))

(def tree-node-fixo
  {:fixo-push (fn [node value]
                (xconj node value))
   :fixo-peek (fn [node]
                (if (:l node)
                  (recur (:l node))
                  (:val node)))
   :fixo-pop (fn [node]
               (if (:l node)
                 (TreeNode. (:val node) (fixo-pop (:l node)) (:r node))
                 (:r node)))})

(extend TreeNode FIXO tree-node-fixo)

(defn fixed-fixo
  ([limit] (fixed-fixo limit []))
  ([limit vector]
   (reify FIXO
     (fixo-push [this value]
       (if (< (count vector) limit)
         (fixed-fixo limit (conj vector value))
         this))
     (fixo-peek [_]
       (peek vector))
     (fixo-pop [_]
       (pop vector)))))

(defrecord TreeNode [val l r]
  FIXO
  (fixo-push [t v]
    (if (< v val)
      (TreeNode. val (fixo-push l v) r)
      (TreeNode. val l (fixo-push r v))))
  (fixo-peek [t]
    (if l
      (fixo-peek l)
      val))
  (fixo-pop [t]
    (if l
      (TreeNode. val (fixo-pop l) r)
      r)))

(def sample-tree2 (reduce fixo-push (TreeNode. 3 nil nil) [5 2 4 6]))
(xseq sample-tree2)

(deftype InfiniteConstant [i]
  clojure.lang.ISeq
  (seq [this]
    (lazy-seq (cons i (seq this)))))

(take 3 (InfiniteConstant. 5))

(deftype TreeNode [val l r]
  FIXO
  (fixo-push [_ v]
    (if (< v val)
      (TreeNode. val (fixo-push l v) r)
      (TreeNode. val l (fixo-push r v))))
  (fixo-peek [_]
    (if l
      (fixo-peek l)
      val))
  (fixo-pop [_]
    (if l
      (TreeNode. val (fixo-pop l) r)
      r))

  clojure.lang.IPersistentStack
  (cons [this v] (fixo-push this v))
  (peek [this] (fixo-peek this))
  (pop [this] (fixo-pop this))
  clojure.lang.Seqable
  (seq [t]
    (concat (seq l) [val] (seq r))))

(extend-type nil
  FIXO
  (fixo-push [t v]
    (TreeNode. v nil nil)))

(def sample-tree2 (into (TreeNode. 3 nil nil) [5 2 4 6]))
(seq sample-tree2)

(defn build-move [& pieces]
          (apply hash-map pieces))
        (build-move :from "e7" :to "e8" :promotion \Q)

(defrecord Move [from to castle? promotion]
  Object
  (toString [this]
    (str "Move " (:from this)
         " to " (:to this)
         (if (:castle? this) " castle"
           (if-let [p (:promotion this)]
             (str " promote to " p)
             "")))))

(str (Move. "e2" "e4" nil nil))
(.println System/out (Move. "e7" "e8" nil \Q))

(defn build-move [& {:keys [from to castle? promotion]}]
          {:pre [from to]}
          (Move. from to castle? promotion))
        (str (build-move :from "e2" :to "e4"))
