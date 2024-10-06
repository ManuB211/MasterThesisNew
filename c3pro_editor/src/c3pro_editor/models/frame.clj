(ns c3pro-editor.models.frame
  (:require [c3pro-editor.gui.frame :refer [c3pro-frame]]
            [cbsbot.core :as object]
            [cbsbot.macros :refer [defobject]]))

(defobject frame
  :tags #{}
  :events #{}
  :behaviours [:make-project-files-panel
               :make-graph-panel
               :reload-components]
  :gui c3pro-frame)
