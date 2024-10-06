(ns c3pro-editor.gui.related
  (require [cbsbot.core :as object]
           [cbsbot.macros :refer [defgui]]
           [seesaw.core :as ss]
           [seesaw.dev :refer [show-options show-events]]
           [seesaw.mig :as ssmig]))

(defgui related-panel-gui
        :init (fn []
                (ss/horizontal-panel
                  :id :related-panel
                  :items [(ss/vertical-panel
                            :items [(ss/label :text "Role:")
                                    (ss/label :text "Node:")
                                    (ss/label :text "The graph goes here")])])))

(defgui related-panel-item-gui
        :init (fn []
                (ss/label :text "bottom related part item")))

;(show-options
  ;(ss/select (object/select-gui (object/select-object :frame)) [:#related-panel]))

