(ns c3pro-editor.config
  (:require [taoensso.nippy :as nippy]))

;; config stuff

(defrecord C3ProConfig [choreography-filename
                        collaboration-filename
                       ])

(defn write-config [filename config]
  (with-open [w (clojure.java.io/output-stream filename)]
    (.write w (nippy/freeze-to-bytes config))))

(defn read-config [filename]
  (let [file filename
        output (byte-array (.length (clojure.java.io/as-file file)))]
    (with-open [input (clojure.java.io/input-stream file)]
      (.read input output))
    (nippy/thaw-from-bytes output)))

(def default-config (C3ProConfig. "../target/BookTripOperation.xml"
                                  "../target/CollaborationBookTripV1.xml"))


