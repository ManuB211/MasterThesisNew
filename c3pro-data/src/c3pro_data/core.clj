(ns c3pro-data.core
  (require [c3pro-data.stats-transform :as stats])
  (require [c3pro-data.log-transform :as log])
  (:gen-class :main true))

;(summarize "ChangePropagationStats.csv" "summary_latex_table.txt")

;(add-target-nodes-mean-column "ChangePropagationStats.csv" "ChangePropagationStats_with_type_source_mean.csv")
;(add-gateway-count-column "ChangePropagationStats_with_type_source_mean.csv" "ChangePropagationStats_with_gateway_count.csv")

;(def default-row-transformations
  ;[;; emit new rows for those with the same initial change id
  ;#(transform-into-rows %
                        ;(fn [row] (nth row INITIAL_CHANGE_ID)) ; group rows we want to transform
                        ;(fn [grouped-rows] ;
                          ;(if (> 1 (count grouped-rows))
                            ;(do
                              ;; actually emit here
                              ;(let [max-response-time (apply max (map (fn [row] (read-string (nth row RESPONSE_TIME))) grouped-rows))
                                    ;new-rows (map (fn [row]
                                                    ;(let [ts (nth row TS) ;; actually parse as a date object
                                                          ;rt (read-string (nth row RESPONSE_TIME))
                                                          ;derived-id (nth row DERIVED_ID)
                                                          ;derived-type (nth row DERIVED_TYPE)
                                                          ;partner (nth row AFFECTED_PARTNER)
                                                          ;ts' (.addSeconds ts rt)
                                                         ;]
                                                    ;[ts' derived-id derived-type partner]))
                                                  ;grouped-rows)
                                   ;]
                                ;new-rows))
                            ;[]
                          ;))
                        ;)])

(defn -main [& args]
  (if (or (nil? args) (< (count args) 3))
    (println "usage: c3pro-data.exe <csv-log-filename> <csv-out-log-filename> <csv-stats-filename> <out-csv-stats-filename-normal> <out-csv-stats-filename-without-noise>")
    (let [csv-log-filename                (first args)
          csv-out-log-filename            (second args)
          csv-stats-filename              (nth args 2)
          csv-out-stats-filename-normal   (nth args 3)
          csv-out-stats-filename-no-noise (nth args 4)]
      (stats/perform-transformation               csv-stats-filename csv-out-stats-filename-normal)
      (stats/perform-transformation-without-noise csv-stats-filename csv-out-stats-filename-no-noise)
      (log/perform-transformation                 csv-log-filename   csv-out-log-filename)
      (stats/summarize                            csv-stats-filename "latex_table_summary.txt")
      (log/generate-communication-table           csv-log-filename   "latex_table_communication.txt")
      )))

;(log/generate-communication-table "ChangePropagationLog.csv" "test.txt")
(-main "ChangePropagationLog.csv" "ChangePropagationLogFinal.csv" "ChangePropagationStats.csv" "ChangePropagationStatsFinal.csv" "ChangePropagationStatsFinalWithoutNoise.csv")
