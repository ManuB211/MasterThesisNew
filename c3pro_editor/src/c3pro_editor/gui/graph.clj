(ns c3pro-editor.gui.graph
  (require [cbsbot.core :as object]
           [cbsbot.macros :refer [defgui]]
           [seesaw.core :as ss]
           [seesaw.mig :as ssmig]))

(defgui graph-panel-gui
        :init (fn []
                (ss/vertical-panel
                  :id :graph-panel
                  :items [])))
