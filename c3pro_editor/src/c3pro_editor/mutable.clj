(ns c3pro-editor.mutable
  (use [c3pro-editor.models.graph]))

;; mutable variables

; NOTE: evaluating everything in this buffer resets the a-f. Need to be careful
; doing so with a running app.
(def a-f      (atom nil)) ;; holds the main window frame
(def a-config (atom nil)) ;; holds the project configuration
(def a-model-cache (atom empty-model-cache)) ;; holds a hashmap from key -> model (public, choreo etc.)

