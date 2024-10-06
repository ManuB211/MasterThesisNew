(ns c3pro-editor.gui.project-explorer
  (require [cbsbot.core :as object]
           [cbsbot.macros :refer [defgui]]
           [seesaw.core :as ss]))

(defgui project-explorer-gui
        :init (fn []
                (ss/flow-panel :id :project-explorer-panel
                               ;:constraints ["" "fill" "fill"]
                               :size [250 :by 300]
                               :items []
                               :border "Project Explorer")))

