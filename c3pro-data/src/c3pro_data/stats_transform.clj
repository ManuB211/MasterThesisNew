(ns c3pro-data.stats-transform
  (require [clojure-csv.core :as csv])
  (require [c3pro-data.transform :as t])
  )

(def PARTNER_COL 1)
(def TYPE_COL 2)
(def NODES_SOURCE_COL 3)
(def ACTIVITY_SOURCE_COL 4)
(def AND_GT_SOURCE_COL 5)
(def XOR_GT_SOURCE_COL 6)
(def NODES_TARGET_COL 7)
(def ACTIVITY_TARGET_COL 8)
(def AND_GT_TARGET_COL 9)
(def XOR_GT_TARGET_COL 10)
(def AFFECTED_PARTNERS_COL 11)
(def INSERT_GENERATED 12)
(def REPLACE_GENERATED 13)
(def DELETE_GENERATED 14)
(def EXEC_TIME 15)
(def AVG_NODES_TARGET_BY_TYPE_SIZE 16)
(def AVG_NODES_TARGET 17)
(def NB_GATEWAY_SOURCE 18)
(def NB_AFFECTING_ROWS 19)
(def NB_AFFECTING_GATEWAY_ROWS 20)
(def NB_AFFECTING_ACTIVITY_ROWS 21)
(def STRUCTURE_TYPE 22)
(def AVG_EXEC_TIME_BY_TYPE 23)
(def TOTAL_EXEC_TIME 24)
(def TOTAL_EXEC_TIME_BY_TYPE 25)
(def IMPACT_RATIO_PER_PARTNER 26)
(def SHORT_PARTNER_NAME 27)
(def MAX_AFFECTED_NODES 28)
(def MAGNITUDE 29)

(defn is-of-type
  [row the-type]
  (= (nth row TYPE_COL) the-type))

(defn summarize-type
  [rows the-type]
  (let [filtered-rows (filter #(is-of-type %1 the-type) rows)
        num-rows (count filtered-rows)
        ; raw data
        nodes-source (t/extract-values filtered-rows NODES_SOURCE_COL)
        activity-source (t/extract-values filtered-rows ACTIVITY_SOURCE_COL)
        and-gt-source (t/extract-values filtered-rows AND_GT_SOURCE_COL)
        xor-gt-source (t/extract-values filtered-rows XOR_GT_SOURCE_COL)
        nodes-target (t/extract-values filtered-rows NODES_TARGET_COL)
        activity-target (t/extract-values filtered-rows ACTIVITY_TARGET_COL)
        and-gt-target (t/extract-values filtered-rows AND_GT_TARGET_COL)
        xor-gt-target (t/extract-values filtered-rows XOR_GT_TARGET_COL)
        affected-partners (t/extract-values filtered-rows AFFECTED_PARTNERS_COL)
        insert-generated (t/extract-values filtered-rows INSERT_GENERATED)
        replace-generated (t/extract-values filtered-rows REPLACE_GENERATED)
        delete-generated (t/extract-values filtered-rows DELETE_GENERATED)
        ; avg calculation
        avg-nodes-source (t/average nodes-source)
        avg-activity-source (t/average activity-source)
        avg-and-gt-source (t/average and-gt-source)
        avg-xor-gt-source (t/average xor-gt-source)
        avg-nodes-target (t/average nodes-target)
        avg-activity-target (t/average activity-target)
        avg-and-gt-target (t/average and-gt-target)
        avg-xor-gt-target (t/average xor-gt-target)
        avg-affected-partners (t/average affected-partners)
        avg-insert-generated (t/average insert-generated)
        avg-replace-generated (t/average replace-generated)
        avg-delete-generated (t/average delete-generated)
        ]
    {:num-rows          num-rows
     :nodes-source      nodes-source
     :activity-source   activity-source
     :and-gt-source     and-gt-source
     :xor-gt-source     xor-gt-source
     :nodes-target      nodes-target
     :activity-target   activity-target
     :and-gt-target     and-gt-target
     :xor-gt-target     xor-gt-target
     :affected-partners affected-partners
     :insert-generated  insert-generated
     :replace-generated replace-generated
     :delete-generated  delete-generated
     ; avg data
     :avg-nodes-source      (float avg-nodes-source)
     :avg-activity-source   (float avg-activity-source)
     :avg-and-gt-source     (float avg-and-gt-source)
     :avg-xor-gt-source     (float avg-xor-gt-source)
     :avg-nodes-target      (float avg-nodes-target)
     :avg-activity-target   (float avg-activity-target)
     :avg-and-gt-target     (float avg-and-gt-target)
     :avg-xor-gt-target     (float avg-xor-gt-target)
     :avg-affected-partners (float avg-affected-partners)
     :avg-insert-generated  (float avg-insert-generated)
     :avg-replace-generated (float avg-replace-generated)
     :avg-delete-generated  (float avg-delete-generated)
     ; max data
     :max-nodes-source      (float (apply max nodes-source))
     :max-activity-source   (float (apply max activity-source))
     :max-and-gt-source     (float (apply max and-gt-source))
     :max-xor-gt-source     (float (apply max xor-gt-source))
     :max-nodes-target      (float (apply max nodes-target))
     :max-activity-target   (float (apply max activity-target))
     :max-and-gt-target     (float (apply max and-gt-target))
     :max-xor-gt-target     (float (apply max xor-gt-target))
     :max-affected-partners (float (apply max affected-partners))
     :max-insert-generated  (float (apply max insert-generated))
     :max-replace-generated (float (apply max replace-generated))
     :max-delete-generated  (float (apply max delete-generated))
     ; min data
     :min-nodes-source      (float (apply min nodes-source))
     :min-activity-source   (float (apply min activity-source))
     :min-and-gt-source     (float (apply min and-gt-source))
     :min-xor-gt-source     (float (apply min xor-gt-source))
     :min-nodes-target      (float (apply min nodes-target))
     :min-activity-target   (float (apply min activity-target))
     :min-and-gt-target     (float (apply min and-gt-target))
     :min-xor-gt-target     (float (apply min xor-gt-target))
     :min-affected-partners (float (apply min affected-partners))
     :min-insert-generated  (float (apply min insert-generated))
     :min-replace-generated (float (apply min replace-generated))
     :min-delete-generated  (float (apply min delete-generated))
     }))

(defn latex-table
  [data filename]
  (let [f #(format "%.2f" (get-in data %1))]
    (with-open [w (clojure.java.io/writer filename)]
      (.write w "\\begin{table*}\\centering\n")
      (.write w "\\ra{1.2}\n")
      (.write w "\\begin{tabular}{@{}rccccccccccc@{}}\\toprule\n")
      (.write w "& & INSERT & & & DELETE & & & REPLACE & \\\\\n")
      (.write w "& min & avg & max & min & avg & max & min & avg & max \\\\ \\midrule\n")
      (.write w "\\textbf{Source}\\\\\n")
      (.write w (str "\\,\\,\\,\\,All nodes & "
                     (f [:insert :min-nodes-source])
                     " & "
                     (f [:insert :avg-nodes-source])
                     " & "
                     (f [:insert :max-nodes-source])
                     " & "
                     (f [:delete :min-nodes-source])
                     " & "
                     (f [:delete :avg-nodes-source])
                     " & "
                     (f [:delete :max-nodes-source])
                     " & "
                     (f [:replace :min-nodes-source])
                     " & "
                     (f [:replace :avg-nodes-source])
                     " & "
                     (f [:replace :max-nodes-source]) " \\\\\n"))
      (.write w (str "\\,\\,\\,\\,Activity nodes & "
                     (f [:insert :min-activity-source])
                     " & "
                     (f [:insert :avg-activity-source])
                     " & "
                     (f [:insert :max-activity-source])
                     " & "
                     (f [:delete :min-activity-source])
                     " & "
                     (f [:delete :avg-activity-source])
                     " & "
                     (f [:delete :max-activity-source])
                     " & "
                     (f [:replace :min-activity-source])
                     " & "
                     (f [:replace :avg-activity-source])
                     " & "
                     (f [:replace :max-activity-source]) " \\\\\n"))
      (.write w (str "\\,\\,\\,\\,AND Gateway nodes & "
                     (f [:insert :min-and-gt-source])
                     " & "
                     (f [:insert :avg-and-gt-source])
                     " & "
                     (f [:insert :max-and-gt-source])
                     " & "
                     (f [:delete :min-and-gt-source])
                     " & "
                     (f [:delete :avg-and-gt-source])
                     " & "
                     (f [:delete :max-and-gt-source])
                     " & "
                     (f [:replace :min-and-gt-source])
                     " & "
                     (f [:replace :avg-and-gt-source])
                     " & "
                     (f [:replace :max-and-gt-source]) "\\\\\n"))
      (.write w (str "\\,\\,\\,\\,XOR Gateway nodes & "
                     (f [:insert :min-xor-gt-source])
                     " & "
                     (f [:insert :avg-xor-gt-source])
                     " & "
                     (f [:insert :max-xor-gt-source])
                     " & "
                     (f [:delete :min-xor-gt-source])
                     " & "
                     (f [:delete :avg-xor-gt-source])
                     " & "
                     (f [:delete :max-xor-gt-source])
                     " & "
                     (f [:replace :min-xor-gt-source])
                     " & "
                     (f [:replace :avg-xor-gt-source])
                     " & "
                     (f [:replace :max-xor-gt-source])
                     "\\\\\n"))
      (.write w "\\midrule\n")
      (.write w "\\textbf{Target}\\\\\n")
      (.write w (str "\\,\\,\\,\\,All nodes & "
                     (f [:insert :min-nodes-target])
                     " & "
                     (f [:insert :avg-nodes-target])
                     " & "
                     (f [:insert :max-nodes-target])
                     " & "
                     (f [:delete :min-nodes-target])
                     " & "
                     (f [:delete :avg-nodes-target])
                     " & "
                     (f [:delete :max-nodes-target])
                     " & "
                     (f [:replace :min-nodes-target])
                     " & "
                     (f [:replace :avg-nodes-target])
                     " & "
                     (f [:replace :max-nodes-target]) "\\\\\n"))
      (.write w (str "\\,\\,\\,\\,Activity nodes & "
                     (f [:insert :min-activity-target])
                     " & "
                     (f [:insert :avg-activity-target])
                     " & "
                     (f [:insert :max-activity-target])
                     " & "
                     (f [:delete :min-activity-target])
                     " & "
                     (f [:delete :avg-activity-target])
                     " & "
                     (f [:delete :avg-activity-target])
                     " & "
                     (f [:replace :min-activity-target])
                     " & "
                     (f [:replace :avg-activity-target])
                     " & "
                     (f [:replace :max-activity-target]) "\\\\\n"))
      (.write w (str "\\,\\,\\,\\,AND Gateway nodes & "
                     (f [:insert :min-and-gt-target])
                     " & "
                     (f [:insert :avg-and-gt-target])
                     " & "
                     (f [:insert :max-and-gt-target])
                     " & "
                     (f [:delete :min-and-gt-target])
                     " & "
                     (f [:delete :avg-and-gt-target])
                     " & "
                     (f [:delete :max-and-gt-target])
                     " & "
                     (f [:replace :min-and-gt-target])
                     " & "
                     (f [:replace :avg-and-gt-target])
                     " & "
                     (f [:replace :max-and-gt-target]) "\\\\\n"))
      (.write w (str "\\,\\,\\,\\,XOR Gateway nodes & "
                     (f [:insert :min-xor-gt-target])
                     " & "
                     (f [:insert :avg-xor-gt-target])
                     " & "
                     (f [:insert :max-xor-gt-target])
                     " & "
                     (f [:delete :min-xor-gt-target])
                     " & "
                     (f [:delete :avg-xor-gt-target])
                     " & "
                     (f [:delete :max-xor-gt-target])
                     " & "
                     (f [:replace :min-xor-gt-target])
                     " & "
                     (f [:replace :avg-xor-gt-target])
                     " & "
                     (f [:replace :max-xor-gt-target]) "\\\\\n"))
      (.write w "\\midrule\n")
      (.write w "\\midrule\n")
      (.write w "& & INSERT & & & DELETE & & & REPLACE & \\\\\n")
      (.write w "& min & avg & max & min & avg & max & min & avg & max \\\\ \\midrule\n")
      (.write w "\\textbf{Generated Operations}\\\\\n")
      (.write w (str "\\,\\,\\,\\,INSERT & "
                     (f [:insert :min-insert-generated])
                     " & "
                     (f [:insert :avg-insert-generated])
                     " & "
                     (f [:insert :max-insert-generated])
                     " & "
                     (f [:insert :min-delete-generated])
                     " & "
                     (f [:insert :avg-delete-generated])
                     " & "
                     (f [:insert :max-delete-generated])
                     " & "
                     (f [:insert :min-replace-generated])
                     " & "
                     (f [:insert :avg-replace-generated])
                     " & "
                     (f [:insert :max-replace-generated])
                     "\\\\\n"))
      (.write w (str "\\,\\,\\,\\,DELETE & "
                     (f [:delete :min-insert-generated])
                     " & "
                     (f [:delete :avg-insert-generated])
                     " & "
                     (f [:delete :max-insert-generated])
                     " & "
                     (f [:delete :min-delete-generated])
                     " & "
                     (f [:delete :avg-delete-generated])
                     " & "
                     (f [:delete :max-delete-generated])
                     " & "
                     (f [:delete :min-replace-generated])
                     " & "
                     (f [:delete :avg-replace-generated])
                     " & "
                     (f [:delete :max-replace-generated])
                     "\\\\\n"))
      (.write w (str "\\,\\,\\,\\,REPLACE & "
                     (f [:replace :min-insert-generated])
                     " & "
                     (f [:replace :avg-insert-generated])
                     " & "
                     (f [:replace :max-insert-generated])
                     " & "
                     (f [:replace :min-delete-generated])
                     " & "
                     (f [:replace :avg-delete-generated])
                     " & "
                     (f [:replace :max-delete-generated])
                     " & "
                     (f [:replace :min-replace-generated])
                     " & "
                     (f [:replace :avg-replace-generated])
                     " & "
                     (f [:replace :max-replace-generated])
                     "\\\\\n"))
      (.write w "\\bottomrule\n")
      (.write w "\\end{tabular}\n")
      (.write w "\\caption{Descriptive Statistics of Simulation Result Data}\n")
      (.write w "\\label{tab:summary}\n")
      (.write w "\\end{table*}"))))

(defn summarize
  "Takes the raw c3pro csv and summarizes the data"
  [filename outfname]
  (let [csv-data (csv/parse-csv (slurp filename))
        rows     (rest csv-data)
        data     {:insert (summarize-type rows "Insert")
                  :delete (summarize-type rows "Delete")
                  :replace (summarize-type rows "Replace")}]
    (latex-table data outfname)
    ))

(defn add-target-nodes-mean-column
  [inf outf]
  (t/calculate-add-column inf outf
                        "avg_nodes_target_by_type_size"
                        ;; key function
                        (fn [row] (vector (nth row TYPE_COL)
                                          (nth row NODES_SOURCE_COL)))
                        ;; calculate function
                        (fn [grouped-rows] (float (t/average (map #(read-string (nth %1 NODES_TARGET_COL)) grouped-rows))))))

(defn add-gateway-count-column
  [inf outf]
  (t/calculate-add-column inf outf
                        "nb_gateway_source"
                        ;; key function
                        (fn [row] row)
                        ;; calculate function
                        (fn [grouped-rows]
                          (let [row (first grouped-rows)
                                and-gateways (read-string (nth row AND_GT_SOURCE_COL))
                                xor-gateways (read-string (nth row XOR_GT_SOURCE_COL))
                               ]
                            (+ and-gateways xor-gateways)))))

(def default-transformations
  [;; add target nodes mean column
   #(t/transform-rows % "avg_nodes_target_by_type_size"
                    ;; key function
                    (fn [row] (vector (nth row TYPE_COL)
                                      (nth row NODES_SOURCE_COL)))
                    ;; calculate function
                    (fn [grouped-rows] (float (t/average (map (fn [row] (read-string (nth row NODES_TARGET_COL))) grouped-rows)))))
   ;; add target nodes mean column
   #(t/transform-rows % "avg_nodes_target"
                    ;; key function
                    (fn [row] (nth row NODES_SOURCE_COL))
                    ;; calculate function
                    (fn [grouped-rows] (float (t/average (map (fn [row] (read-string (nth row NODES_TARGET_COL))) grouped-rows)))))
   ;; add gateway count
   #(t/transform-rows % "nb_gateway_source"
                    ;; key function
                    (fn [row] row)
                    ;; calculate function
                    (fn [grouped-rows]
                      (let [row (first grouped-rows)
                            and-gateways (read-string (nth row AND_GT_SOURCE_COL))
                            xor-gateways (read-string (nth row XOR_GT_SOURCE_COL))
                           ]
                        (+ and-gateways xor-gateways))))
   ;; add # of rows that affect this nb_nodes_target (grouped by type)
   #(t/transform-rows % "nb_affecting_rows"
                    (fn [row] (vector (nth row TYPE_COL)
                                      (nth row NODES_TARGET_COL)))
                    (fn [grouped-rows] (count grouped-rows)))
   ;; add average nb_nodes_target for gateway count
   #(t/transform-rows % "avg_nodes_affected_gateway"
                    (fn [row] (nth row NB_GATEWAY_SOURCE))
                    (fn [grouped-rows] (float (t/average (map (fn [row] (read-string (nth row NODES_TARGET_COL))) grouped-rows)))))
   ;; add average nb_nodes_target for activity count
   #(t/transform-rows % "avg_nodes_affected_activity"
                    (fn [row] (nth row ACTIVITY_SOURCE_COL))
                    (fn [grouped-rows] (float (t/average (map (fn [row] (read-string (nth row NODES_TARGET_COL))) grouped-rows)))))
   ;; flag the source structure as "Sequence" or "Complex"
   #(t/transform-rows % "structure_type"
                    (fn [row] row)
                    (fn [grouped-rows] (let [row (first grouped-rows)
                                             activity_count (read-string (nth row NB_GATEWAY_SOURCE))]
                                         (if (= activity_count 0)
                                           "Sequence"
                                           "Complex"))))
   ;; calculate the average execution time by type
   #(t/transform-rows % "avg_exec_time_by_type"
                    (fn [row] (vector (nth row TYPE_COL)
                                      (nth row NODES_SOURCE_COL)))
                    (fn [grouped-rows] (float (t/average (map (fn [row] (read-string (nth row EXEC_TIME))) grouped-rows)))))
   ;; calculate total execution time
   #(t/transform-rows % "total_exec_time"
                    (fn [row] identity) ;; to sum over all rows
                    (fn [grouped-rows] (reduce + 0 (map (fn [row] (read-string (nth row EXEC_TIME))) grouped-rows))))
   ;; calculate total execution time by type
   #(t/transform-rows % "total_exec_time_by_type"
                    (fn [row] (nth row TYPE_COL))
                    (fn [grouped-rows] (reduce + 0 (map (fn [row] (read-string (nth row EXEC_TIME))) grouped-rows))))
   #(t/transform-rows % "impact_ratio_per_partner"
                      (fn [row] (vector (nth row PARTNER_COL)
                                        (nth row TYPE_COL)))
                      (fn [grouped-rows]
                        (let [n   (count grouped-rows)
                              sum (reduce + 0
                                          (map (fn [row] (let [affected-nodes (read-string (nth row NODES_TARGET_COL))
                                                               source-nodes   (read-string (nth row NODES_SOURCE_COL))]
                                                               (float (/ affected-nodes source-nodes)))) grouped-rows))]
                          (/ sum n))))
   #(t/transform-rows % "short_partner_name"
                      (fn [row] (nth row PARTNER_COL))
                      (fn [grouped-rows]
                        (let [first-row (first grouped-rows)
                              partner-name (nth first-row PARTNER_COL)]
                          (if (.startsWith partner-name "partner")
                            ;; we have a partner here
                            (let [numbers (set (map str (range 10)))
                                  partner-number (clojure.string/join (filter (fn [x] (contains? numbers (str x))) partner-name))
                                  ]
                              (str "p" partner-number))
                            ;; we have either Acquirer, Airline, Traveler or TravelAgency
                            (let [uppercases (clojure.string/join (filter (fn [x] (Character/isUpperCase x)) partner-name))
                                  final-name (if (= 2 (count uppercases))
                                               uppercases
                                               (subs partner-name 0 2))]
                              final-name)))))
   #(t/transform-rows % "max_affected_nodes"
                      (fn [row] identity)
                      (fn [grouped-rows] (let [affected-nodes     (map (fn [row] (read-string (nth row NODES_TARGET_COL))) grouped-rows)
                                               max-affected-nodes (apply max affected-nodes)]
                                           (float max-affected-nodes))))
   #(t/transform-rows % "magnitude"
                      (fn [row] row) ;; TODO: why not identity? Why does identity work over all rows?
                      ; TODO: adjust the magnitudes properly
                      (fn [grouped-rows] (let [determine-magnitude' (fn [x limit]
                                                                     (let [steps (/ limit 4)]
                                                                       (if (< x steps) "xs"
                                                                         (if (< x (* steps 2)) "s"
                                                                           (if (< x (* steps 3)) "m"
                                                                             "l")))))
                                               determine-magnitude (fn [x limit]
                                                                     (if (< x 5.0) "xs"
                                                                       (if (< x 15.0) "s"
                                                                         (if (< x 25.0) "m"
                                                                           "l"))))
                                               row                (first grouped-rows)
                                               max-affected-nodes (read-string (nth row MAX_AFFECTED_NODES))
                                               affected-nodes     (read-string (nth row NODES_TARGET_COL))
                                               magnitude          (determine-magnitude affected-nodes max-affected-nodes)]
                                               magnitude)))
   ])

(defn remove-replace-noise
  "We remove those rows which represent a replace operation that replaces a
  fragment with an identical one. This is in effect a NOOP."
  [row]
  (not (= 0
     (read-string (nth row NODES_TARGET_COL))
     (read-string (nth row ACTIVITY_TARGET_COL))
     (read-string (nth row AND_GT_TARGET_COL))
     (read-string (nth row XOR_GT_TARGET_COL))
     (read-string (nth row AFFECTED_PARTNERS_COL))
     (read-string (nth row INSERT_GENERATED))
     (read-string (nth row REPLACE_GENERATED))
     (read-string (nth row DELETE_GENERATED)))))

(defn perform-transformation [filein fileout]
  (t/csv-transform filein fileout
           [] ; pre-filter
           default-transformations ; transformations
           [] ; post-filter
           ))

(defn perform-transformation-without-noise [filein fileout]
  (t/csv-transform filein fileout
                 [remove-replace-noise] ; pre-filter
                 default-transformations ; column transformations
                 [] ; post-filter
                 ))

