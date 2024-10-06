(ns c3pro-editor.models.related
  (require [cbsbot.core :as object]
           [cbsbot.macros :refer [defobject]]
           [c3pro-editor.gui.related :refer [related-panel-gui
                                             related-panel-item-gui]]))

(defobject related-panel
  :tags #{:container :hideable}
  :events #{}
  :behaviours [:display-related-nodes]
  :gui related-panel-gui)

(defobject related-panel-item
  :tags #{:container-item}
  :events #{:jgraph.node-selected
            :jgraph.node-deselected}
  :behaviours [:render-model]
  :gui related-panel-item-gui)
