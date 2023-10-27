(ns clojure-sandbox.joc-workbook.joc-8
  (:require
   [clojure.walk :refer [macroexpand-all]]))

(defmacro do-until [& clauses]
  `(let [result (first ~clauses)]
     (if result (print (rest ~clauses)) nil)))

(defmacro do-until [& clauses]
  (if (eval (first clauses))
    (if (next clauses)
      `(do
         ~(second clauses)
         (do-until ~@(nnext clauses))) nil)
    nil))
(comment (macroexpand-1 '(do-until
                          (even? 2) (println "Even")
                          (odd?  3) (println "Odd")
                          (zero? 1) (println "You never see me")
                          :lollipop (println "Truthy thing"))))

(comment (macroexpand-all '(do-until true (prn 1) false (prn 2))))

(defmacro unless [condition & body]
  `(if (not ~condition) (do ~@body) nil))

(comment (macroexpand-all (unless (even? 3) "Now we see it..." "Second line")))

(macroexpand-1 (do
                 (println "Even")
                 (do-until
                  ((odd? 3)
                   (println "Odd")
                   (zero? 1)
                   (println "You never see me")
                   :lollipop
                   (println "Truthy thing")))))

(defmacro def-watched [name & value]
  `(do
     (def ~name ~@value)
     (add-watch (var ~name)
                :re-bind
                (fn [~'key ~'r old# new#]
                  (println old# " -> " new#)))))
(do (def x 2)
    (add-watch (var x)
               :re-bind
               (fn [key r old new]
                 (println old " -> " new))))

(def-watched x (* 12 12))
(def x 0)

(defmacro domain [name & body]
  `{:tag :domain,
    :attrs {:name (str '~name)},
    :content [~@body]})

(declare handle-things)
(defmacro grouping [name & body]
  `{:tag :grouping,
    :attrs {:name (str '~name)},
    :content [~@(handle-things body)]})

(declare grok-attrs grok-props)
(defn handle-things [things]
  (for [t things]
    {:tag :thing,
     :attrs (grok-attrs (take-while (comp not vector?) t))
     :content (if-let [c (grok-props (drop-while (comp not vector?) t))]
                [c] [])}))

(defn grok-attrs [attrs]
  (into {:name (str (first attrs))}
        (for [a (rest attrs)]
          (cond
            (list? a) [:isa (str (second a))]
            (string? a) [:comment a]))))

(defn grok-props [props]
  (when props
    {:tag :properties, :attrs nil,
     :content (apply vector (for [p props]
                              {:tag :property,
                               :attrs {:name (str (first p))},
                               :content nil}))}))

(defmacro awhen [expr & body]
  `(let [~'it ~expr]
     (if ~'it
       (do ~@body))))

(awhen [1 2 3] (it 2))
(awhen nil (println "Will never get here"))
(awhen 1 (awhen 2 [it]))

; (import [java.io BufferedReader InputStreamReader]
;         [java.net URL])
;
; (defn joc-www []
;   (-> "http://joyofclojure.com/hello" URL.
;       .openStream
;       InputStreamReader.
;       BufferedReader.))
;
; (let [stream (joc-www)]
;   (with-open [page stream]
;     (println (.readLine page))
;     (print "The stream will now close... "))
;   (println "but let's read from it anyway.")
;   (.readLine stream))
;
; (defmacro with-resource [binding close-fn & body]
;   `(let ~binding
;      (try
;        (do ~@body)
;        (finally
;          (~close-fn ~(binding 0))))))
;
; (let [stream (joc-www)]
;   (with-resource [page stream]
;     #(.close %)
;     (.readLine page)))

(declare collect-bodies)

(declare build-contract)
(defn collect-bodies [forms]
  (for [form (partition 3 forms)]
    (build-contract form)))

(defn build-contract [c]
  (let [args (first c)]
    (list
     (into '[f] args)
     (apply merge
            (for [con (rest c)]
              (cond (= (first con) 'require)
                    (assoc {} :pre (vec (rest con)))
                    (= (first con) 'ensure)
                    (assoc {} :post (vec (rest con)))
                    :else (throw (Exception.
                                  (str "Unknown tag "
                                       (first con)))))))
     (list* 'f args))))
