(ns c3pro-data.log-transform
  (require [c3pro-data.transform :as t])
  (require [clj-time.core :as ct]
           [clj-time.format :as ctformat])
  (require [clojure-csv.core :as csv])
  )

(def TIMESTAMP_COL 0)
(def INITIAL_CHANGE_ID 1)
(def INITIAL_CHANGE_TYPE 2)
(def CHANGE_REQUESTOR 3)
(def NB_SOURCE_NODES 4)
(def AFFECTED_PARTNER 5)
(def DERIVED_CHANGE_ID 6)
(def DERIVED_CHANGE_TYPE 7)
(def DERIVED_NB_NODES 8)
(def NEGOTIATION_RESULT 9)
(def RESPONSE_TIME 10)
(def CALCULATED_TIMESTAMP 11)
(def OUT_DEGREE 12)
(def IN_DEGREE 13)

(defn read-number [x]
  (-> (drop-while #(= \0 %) x)
      ((fn [x] (read-string (clojure.string/join "" x))))))

(def custom-formatter (ctformat/formatter "YYYY-MM-dd HH:mm:ss.SSS"))

(defn perform-transformation [filein fileout]
  (t/csv-transform filein fileout
                 [] ; pre-filters
                 [#(t/transform-rows % "Calculated Timestamp"
                                     (fn [row] row)
                                     (fn [grouped-rows]
                                       (let [row (first grouped-rows)
                                             ts  (-> (nth row TIMESTAMP_COL)
                                                     ((fn [s] (ctformat/parse custom-formatter s))))
                                             rt  (read-string (nth row RESPONSE_TIME))
                                             ts' (ct/plus ts (ct/seconds rt))
                                            ]
                                         (ctformat/unparse custom-formatter ts')
                                         )))
                  #(t/transform-rows % "out_degree"
                                     (fn [row] (nth row CHANGE_REQUESTOR))
                                     (fn [grouped-rows]
                                       (count grouped-rows)))
                  #(t/transform-rows % "in_degree"
                                     (fn [row] (nth row AFFECTED_PARTNER))
                                     (fn [grouped-rows]
                                       (count grouped-rows)))
                  ]
                 []))

;(perform-transformation "log_transformed.csv" "test.csv")

(defn generate-communication-table [log-filename table-out-filename]
  (let [csv-data (csv/parse-csv (slurp log-filename))
        labels (first csv-data)
        rows   (rest csv-data)
        partners (concat ["Airline", "Acquirer", "Traveler", "TravelAgency"]
                         (map #(str "partner" %) (range 1 21)))
        grouped-by-requestor (group-by (fn [row] (nth row CHANGE_REQUESTOR)) rows)
        indexed (apply hash-map (apply concat
                                       (for [[k v] grouped-by-requestor]
                                         [k (map #(nth % AFFECTED_PARTNER) v)])))
        count-f (fn [old-v]
                  (apply hash-map (apply concat
                                         (for [[k v] (group-by identity old-v)]
                                           [k (count v)]))))
        ; final communication (raw count)
        ; { partnerX { partnerX1: countX1
        ;            , partnerX2: countX2
        ;            ...
        ;            }
        ; , ...
        ; }
        indexed' (apply hash-map (apply concat
                                        (for [[k v] indexed]
                                          [k (count-f v)])))
        total-count  (fn [m] (reduce + (map #(reduce + (vals %)) (vals m))))
        total-count' (total-count indexed')
        ; add relative count to total-count for each slot
        indexed'' (apply hash-map (apply concat
                                         (for [[k v] indexed']
                                           [k (apply hash-map (apply concat
                                                                  (for [[k' v'] v]
                                                                    [k' {:count v'
                                                                         :relative (float (/ v' total-count'))
                                                                        }])))])))
        ; function to transform the hash-map into this form [[count from_partner to_partner], ...]
        map-to-tuple (fn [m]
                       (apply concat
                         (for [[from_partner v1] m]
                           (for [[to_partner m2] v1]
                             [(m2 :count) (m2 :relative) from_partner to_partner]))))

        latex-table (fn [partners data filename]
                        (with-open [w (clojure.java.io/writer filename)]
                          (do
                            (.write w "\\begin{table*}\\centering\n")
                            (.write w "\\ra{1.2}\n")
                            (.write w "\\begin{tabular}{rrrrrrrrrrrrrrrrrrrrrrrrr}\\toprule\n")
                            (.write w "\\midrule\n")
                            (.write w "& & & & & \\textbf{Partners} & & & & & & & & & & & & & & & & & & & \\\\\n")
                            (.write w "& Ai & Ac & Tr & TA & 1 & 2 & 3 & 4 & 5 & 6 & 7 & 8 & 9 & 10 & 11 & 12 & 13 & 14 & 15 & 16 & 17 & 18 & 19 & 20 \\\\ \\midrule\n")
                            (doseq [p1 partners]
                              (do
                                ;Ai \\
                                ;Ac \\
                                ;Tr \\
                                ;TA \\
                                ;\textbf{Partners} 1 \\
                                ;2 \\
                                ;3 \\
                                ;4 \\
                                ;5 \\
                                ;6 \\
                                ;7 \\
                                ;8 \\
                                ;9 \\
                                ;10 \\
                                ;11 \\
                                ;12 \\
                                ;13 \\
                                ;14 \\
                                ;15 \\
                                ;16 \\
                                ;17 \\
                                ;18 \\
                                ;19 \\
                                ;20 \\
                                (.write w p1)
                                (doseq [p2 partners]
                                  (let [value (or (get-in data [p1 p2 :count]) 0)
                                        value' (* 100 value)]
                                    (.write w " & ")
                                    (.write w (str value))
                                    ))
                                (.write w " \\\\ \n")
                                ))
                            (.write w "\\bottomrule\n")
                            (.write w "\\end{tabular}\n")
                            (.write w "\\caption{Communication}\n")
                            (.write w "\\label{tab:communication}\n")
                            (.write w "\\end{table*}\n")
                            )))
        top-communication-table (fn [total n data filename]
                                  (let [data' (take n (reverse (sort-by first (map-to-tuple data))))
                                        data'' (map vector (range 1 (+ (count data') 1)) data')
                                       ]
                                    (with-open [w (clojure.java.io/writer filename)]
                                      (do
                                        (.write w "\\begin{table}\\centering\n")
                                        (.write w "\\ra{1.2}\n")
                                        (.write w "\\begin{tabular}{rllcr}\\toprule\n")
                                        (.write w "& \\textbf{From} & \\textbf{To} & \\textbf{Frequency} & \\textbf{\\%} \\\\ \\midrule\n")
                                        (doseq [[row [_count relative from to]] data'']
                                          (.write w (str row " & " from " & " to " & " _count " & " (format "%.2f" (* relative 100)) "\\%" "\\\\ \n")))
                                        (.write w "\\bottomrule\n")
                                        (.write w "\\end{tabular}\n")
                                        (.write w (str "\\caption{Top 10 Change Requests sorted by frequency (N=" total ")}\n"))
                                        (.write w "\\label{tab:top_change_requests}\n")
                                        (.write w "\\end{table}\n")
                                    ))))
        ]
    (top-communication-table total-count' 10 indexed'' table-out-filename)))
    ;(print (take 10 (reverse (sort-by first (map-to-tuple indexed'')))))))
    ;(latex-table partners indexed'' table-out-filename)
