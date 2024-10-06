(ns c3pro-editor.system
  (use [c3pro-editor.mutable]
       [c3pro-editor.widgets]
       [c3pro-editor.behaviours.all]
       [seesaw.core])
  (require [c3pro-editor.models.frame :as frame-model]
           [cbsbot.core :as object]))

(defn start-frame []
  (let [f (object/select-gui (object/create frame-model/frame))]
    (doto f
      ;(pack!)
      (show!))
    f))

(defn kill-frame [the-frame]
  (doto the-frame
    (.setVisible false)
    (.dispose)))

(defn start-app
  []
  (do
    (swap! a-f (fn [_] (start-frame)))
    (object/emit-event :frame.reload (object/select-object :frame))))

(defn stop-app
  []
  (do
    (kill-frame @a-f)
    (object/destroy-objects)
    (swap! a-f (fn [_] nil))))

(defn reload-app
  []
  (do
    (stop-app)
    (start-app)))

;; (start-app)
;; (reload-app)
;; (object/emit-event :frame.reload (object/select-object :frame))
;; (stop-app)

;(seesaw.dev/show-options (select @a-f [:#project-explorer-panel]))

;(config (select @a-f [:#project-explorer-panel]) :size)

;(config (select @a-f [:#project-files-tree]) :vgap)

(defn identify
  [root]
  (doseq [w (select root [:*])]
    (if-let [n (.getName w)]
      (seesaw.selector/id-of! w (keyword n))))
  root)

(defn temporary-fragment-dialog-form
  []
  (let [form (identify (c3pro_editor.ChangeFrame.))]
    form))

; (-> (temporary-fragment-dialog-form)
;     pack! show!)

;(import (c3pro-editor ChangeFraame))
