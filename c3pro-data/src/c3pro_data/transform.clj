(ns c3pro-data.transform
  (require [clojure-csv.core :as csv]))

(defn average [coll]
  (/ (reduce + coll) (count coll)))

(defn extract-values
  [rows col-key]
  (map #(read-string (nth %1 col-key)) rows))

(defn transform-rows
  "transform-rows adds a new column to each row indexed by key-f by applying calc-f on the indexed rows"
  [csv-data new-label key-f calc-f]
  (let [rows (rest csv-data)
        labels (conj (first csv-data) new-label)
        grouped-by-key (group-by key-f rows)
        calc-indexed (apply hash-map (apply concat
                                       (for [[k v] grouped-by-key]
                                         [k (calc-f v)])))
        rows' (for [row rows]
                (let [value (str (calc-indexed (key-f row)))]
                  (conj row value)))
        csv-data' (conj rows' labels)]
    csv-data'))

(defn transform-into-rows
  "transform-into-rows adds new rows from the rows indexed by key-f by applying"
  [csv-data ])

(defn calculate-add-column
  "calculate a column from grouped/indexed values and add it to the csv as a new file"
  [inf outf new-label key-f calc-f]
  (let [csv-data  (csv/parse-csv (slurp inf))
        csv-data' (transform-rows csv-data new-label key-f calc-f)
        csv-out   (csv/write-csv csv-data')
        ]
      (spit outf csv-out)))



(defn csv-transform [inf outf pre-filter-fs transform-fs post-filter-fs]
  (let [csv-data  (csv/parse-csv (slurp inf))
        csv-data' (reduce (fn [data f]
                            (filter f data))
                          csv-data
                          pre-filter-fs)
        csv-data'' (reduce (fn [data f]
                             (f data))
                           csv-data'
                           transform-fs)
        csv-data''' (reduce (fn [data f]
                              (filter f data))
                            csv-data''
                            post-filter-fs)
        csv-out   (csv/write-csv csv-data''')]
    (spit outf csv-out)))
