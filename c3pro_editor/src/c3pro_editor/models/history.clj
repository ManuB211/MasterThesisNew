(ns c3pro-editor.models.history
  (require [c3pro-editor.gui.history :refer [history-panel-gui]]
           [cbsbot.macros :refer [defobject]]))

(defobject history-panel
  :tags #{}
  :singleton true
  :events #{}
  :behaviours []
  :gui history-panel-gui)

