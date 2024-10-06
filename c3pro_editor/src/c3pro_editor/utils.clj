(ns c3pro-editor.utils
  (import (java.awt.event MouseEvent)))

(defn double-click? [e]
  (and (= (.getButton e) MouseEvent/BUTTON1) (= 2 (.getClickCount e))))

