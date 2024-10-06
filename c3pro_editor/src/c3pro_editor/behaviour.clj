(ns c3pro-editor.behaviour
  (use [c3pro-editor.config]
       [c3pro-editor.mutable]
       [c3pro-editor.widgets]
       [c3pro-editor.pure]
       [c3pro-editor.models.project-explorer]
       [seesaw.chooser]
       [seesaw.core]
       [seesaw.tree]
       )
  (import (java.awt.event MouseEvent)
          (at.ac.c3pro.chormodel Role)))

;(declare make-graph-panel)
;(declare make-project-files-panel)
;(declare add-behaviours)

;(defn reload-components
  ;)

;;; TODO refactor common elements: load-choreo-action-handler and load-collab-action-handler
;(defn load-choreo-action-handler [e]
  ;)

;(defn load-collab-action-handler
  ;[e]
  ;)

;;; TODO: check for unsaved changes before doing this
;(defn new-project-action-handler [e]
  ;)

;;; TODO: choose default file (last loaded?)
;(defn save-project-action-handler [e]
  ;)

;(defn load-project-action-handler [e]
  ;; 1) file chooser
  ;; 2) read-config of chosen file
  ;; 3) if success, reload all components with new config
  ;)

;(defn select-project-file-handler [e]
  ;)

;(defn add-behaviours [root]
  
  ;root)

;;; graph panel

;(defn make-graph-panel [f]
  ;)

;;; project explorer panel

;(defn make-project-files-panel [f config model-cache]
  ;)

