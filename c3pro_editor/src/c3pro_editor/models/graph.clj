(ns c3pro-editor.models.graph
  (require [cbsbot.core :as object]
           [cbsbot.macros :refer [defobject]]
           [c3pro-editor.gui.graph :refer [graph-panel-gui]]))

(defobject graph-panel
  :tags #{}
  :singleton true
  :events #{:jgraph.node-selected
            :jgraph.node-deselected}
  :behaviours [:render-graph
               :display-node-inspector]
  :gui graph-panel-gui
  ; reference to the main graph component
  :graph-component nil)

(def empty-model-cache {})

