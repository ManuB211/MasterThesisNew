(ns c3pro-editor.models.detail
  (require [cbsbot.core :as object]
           [cbsbot.macros :refer [defobject]]
           [c3pro-editor.gui.detail :refer [detail-panel-gui]]))

(defobject detail-panel
  :tags #{:table :hideable}
  :events #{}
  :singleton true
  :behaviours [:display-node-details
               :reset-detail-panel]
  :gui detail-panel-gui)

