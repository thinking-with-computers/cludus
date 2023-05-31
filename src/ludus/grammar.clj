(ns ludus.grammar
 	(:require [ludus.parser-new :refer :all]
  		[ludus.scanner :as scan]))

(declare expression pattern)

;(def separator (choice :separator [:comma :newline :break]))
(defp separator choice [:comma :newline :break])

;(def separators (quiet (one+ separator)))
(defp separators quiet one+ separator)

;(def terminator (choice :terminator [:newline :semicolon :break]))
(defp terminator choice [:newline :semicolon :break])

;(def terminators (quiet (one+ terminator)))
(defp terminators quiet one+ terminator)

;(def nls? (quiet (zero+ :nls :newline)))
(defp nls? quiet zero+ :newline)

;(def splat (group (order-1 :splat [(quiet :splat) :word])))
(defp splat group order-1 [(quiet :splat) :word])

;(def splattern (group (order-1 :splat [(quiet :splat) (maybe (flat (choice :splatted [:word :ignored :placeholder])))])))
(defp patt-splat-able flat choice [:word :ignored :placeholder])
(defp splattern group order-1 [(quiet :splat) (maybe patt-splat-able)])

;(def literal (flat (choice :literal [:nil :true :false :number :string])))
(defp literal flat choice [:nil :true :false :number :string])

;(def tuple-pattern-term (flat (choice :tuple-pattern-term [pattern splattern])))
(defp tuple-pattern-term flat choice [pattern splattern])

;(def tuple-pattern-entry (weak-order :tuple-pattern-entry [tuple-pattern-term separators]))
(defp tuple-pattern-entry weak-order [tuple-pattern-term separators])

(defp tuple-pattern group order-1 [(quiet :lparen)
                                 		(quiet (zero+ separator))
                                 		(zero+ tuple-pattern-entry)
                                 		(quiet :rparen)])

(defp list-pattern group order-1 [(quiet :lbracket)
                                 	(quiet (zero+ separator))
                                 	(zero+ tuple-pattern-entry)
                                 	(quiet :rbracket)])

(defp pair-pattern order-0 [:keyword #'pattern])

(defp dict-pattern-term flat choice [pair-pattern :word splattern])

(defp dict-pattern-entry weak-order [dict-pattern-term separators])

(defp dict-pattern group order-1 [(quiet :startdict)
                                	 (quiet (zero+ separator))
                                	 (zero+ dict-pattern-entry)
                                	 (quiet :rbrace)
                                 	])

(defp struct-pattern group order-1 [(quiet :startstruct)
                                   	(quiet (zero+ separator))
                                   	(zero+ dict-pattern-entry)
                                   	(quiet :rbrace)
                                   	])

(defp guard order-0 [(quiet :when) expression])

(defp typed group weak-order [:word (quiet :as) :keyword])

(defp pattern flat choice [literal 
                           :ignored 
                           :placeholder 
                           typed 
                           :word 
                           :keyword 
                           tuple-pattern 
                           dict-pattern 
                           struct-pattern 
                           list-pattern])

(defp match-clause group weak-order :match-clause [pattern (maybe guard) (quiet :rarrow) expression])

(defp match-entry weak-order [match-clause terminators])

(defp match group order-1 [(quiet :match) expression nls? 
                          	(quiet :with) (quiet :lbrace)
                          	(quiet (zero+ terminator))
                          	(one+ match-entry)
                          	(quiet :rbrace)
                          	])

(defp if-expr group order-1 [(quiet :if) 
                             nls? 
                             expression 
                             nls? 
                             (quiet :then) 
                             expression 
                             nls? 
                             (quiet :else) 
                             expression])

(defp cond-lhs flat choice [expression :placeholder :else])

(defp cond-clause group weak-order [cond-lhs (quiet :rarrow) expression])

(defp cond-entry weak-order [cond-clause terminators])

(defp cond-expr group order-1 [(quiet :cond) (quiet :lbrace)
                               (quiet (zero+ terminator))
                               (one+ cond-entry)
                               (quiet :rbrace)])

(defp let-expr group order-1 [(quiet :let)
                             	pattern
                             	(quiet :equals)
                             	nls?
                             	expression])

(defp tuple-entry weak-order [expression separators])
 
(defp tuple group order-1 [(quiet :lparen)
                         		(quiet (zero+ separator))
                         		(zero+ tuple-entry)
                         		(quiet :rparen)])

(defp list-term flat choice [splat expression])

(defp list-entry order-1 [list-term separators])

(defp list-literal group order-1 [(quiet :lbracket)
                                		(quiet (zero+ separator))
                                		(zero+ list-entry)
                                		(quiet :rbracket)])

(defp set-literal group order-1 [(quiet :startset)
                                	(quiet (zero+ separator))
                                	(zero+ list-entry)
                                	(quiet :rbrace)])

(defp pair group order-0 [:keyword expression])

(defp struct-term flat choice [:word pair])

(defp struct-entry order-1 [struct-term separators])

(defp struct-literal group order-1 [(quiet :startstruct)
                                   	(quiet (zero+ separator))
                                   	(zero+ struct-entry)
                                   	(quiet :rbrace)])

(defp dict-term flat choice [splat :word pair])

(defp dict-entry order-1 [dict-term separators])

(defp dict group order-1 [(quiet :startdict)
                         	(quiet (zero+ separator))
                         	(zero+ dict-entry)
                         	(quiet :rbrace)])

(defp arg-expr flat choice [:placeholder expression])

(defp arg-entry weak-order [arg-expr separators])

(defp args group order-1 [(quiet :lparen)
                          (quiet (zero+ separator))
                          (zero+ arg-entry)
                          (quiet :rparen)])

(defp recur-call group order-1 [(quiet :recur) tuple])

(defp synth-root flat choice [:keyword :word])

(defp synth-term flat choice [args :keyword])

(defp synthetic group order-1 [synth-root (zero+ synth-term)])

(defp fn-clause group order-1 [tuple-pattern (maybe guard) (quiet :rarrow) expression])

(defp fn-entry order-1 [fn-clause terminators])

(defp compound group order-1 [(quiet :lbrace)
                            		nls?
                             	(maybe :string)
                             	(quiet (zero+ terminator))
                             	(one+ fn-entry)
                             	(quiet :rbrace)
                             	])

(defp clauses flat choice [fn-clause compound])

(defp named group order-1 [:word clauses])

(defp body flat choice [fn-clause named])

(defp fn-expr group order-1 [(quiet :fn) body])

(defp block-line weak-order [expression terminators])

(defp block group order-1 [(quiet :lbrace) 
                          	(quiet (zero+ terminator))
                          	(one+ block-line)
                          	(quiet :rbrace)])

(defp pipeline quiet order-0 [nls? :pipeline])

(defp do-entry order-1 [pipeline expression])

(defp do-expr group order-1 [(quiet :do)
                            	expression
                            	(one+ do-entry)
                            	])

(defp ref-expr group order-1 [(quiet :ref) :word (quiet :equals) expression])

(defp spawn group order-1 [(quiet :spawn) expression])

(defp receive group order-1 [(quiet :receive) (quiet :lbrace)
                            	(quiet (zero+ terminator))
                            	(one+ match-entry)
                            	(quiet :rbrace)
                            	])

(defp compound-loop group order-0 [(quiet :lbrace)
                                 		(quiet (zero+ terminator))
                                 		(one+ fn-entry)
                                  	(quiet :rbrace)])

(defp loop-expr group order-1 [(quiet :loop) tuple (quiet :with)
                              	(flat (choice :loop-body [fn-clause compound-loop]))])

(defp expression flat choice [fn-expr
                             	match
                             	loop-expr
                             	let-expr
                             	if-expr 
                             	cond-expr
                             	spawn
                             	receive
                             	synthetic
                              recur-call
                             	block 
                             	do-expr
                             	ref-expr
                             	struct-literal
                             	dict
                             	list-literal
                             	set-literal
                             	tuple 
                             	literal])

(defp test-expr group order-1 [(quiet :test) :string expression])

(defp import-expr group order-1 [(quiet :import) :string (quiet :as) :word])

(defp ns-expr group order-1 [(quiet :ns) 
                            	:word 
                            	(quiet :lbrace)
                            	(quiet (zero+ separator))
                            	(zero+ struct-entry)
                            	(quiet :rbrace)])

(defp toplevel flat choice [import-expr
                            ns-expr
                            expression 
                            test-expr])

(defp script-line weak-order [toplevel terminators])

(defp script order-0 [nls?
                      (one+ script-line)
                      (quiet :eof)])


;;; REPL

(comment

  (def source 
    "if 1 then 2 else 3"
    )

  (def rule (literal))

  (def tokens (-> source scan/scan :tokens))

  (def result (apply-parser script tokens))


  (defn report [node] 
    (when (fail? node) (err-msg node))  
    node)   

  (defn clean [node]  
    (if (map? node) 
      (-> node    
        (report)    
        (dissoc     
          ;:status     
          :remaining  
          :token) 
        (update :data #(into [] (map clean) %)))    
      node))  

  (defn tap [x] (println "\n\n\n\n******NEW RUN\n\n:::=> " x "\n\n") x)   

  (def my-data (-> result     
                 clean   
                 tap 
                 ))

  (println my-data))