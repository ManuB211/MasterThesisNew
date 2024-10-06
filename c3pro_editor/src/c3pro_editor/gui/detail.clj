(ns c3pro-editor.gui.detail
  (require [cbsbot.core :as object]
           [cbsbot.macros :refer [defgui]]
           [seesaw.mig :as ssmig]))

(defgui detail-panel-gui
        :init (fn []
                (ssmig/mig-panel :id :detail-panel
                                 :constraints ["fillx" "[right]rel[grow, fill]" "[]2[]"]
                                 :items []
                                 :border "Node Inspector"
                                 :size [250 :by 400])))
