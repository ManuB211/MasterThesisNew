(ns c3pro-editor.gui.frame
  (require [cbsbot.macros :refer [defgui]]
           [cbsbot.core :as object]
           [c3pro-editor.models.graph :refer [graph-panel]]
           [c3pro-editor.models.related :refer [related-panel]]
           [c3pro-editor.models.history :refer [history-panel]]
           [c3pro-editor.models.project-explorer :refer [project-explorer]]
           [c3pro-editor.models.detail :refer [detail-panel]]
           [c3pro-editor.models.menu :refer [menu]]
           [seesaw.core :as ss]))

(defgui c3pro-frame
        :init (fn []
                (ss/frame :title "C3Pro"
                          :size [800 :by 600]
                          :on-close :exit
                          :menubar (let [menu-obj (object/create menu)]
                                     (object/select-gui menu-obj))
                          :content (ss/border-panel
                                     :center (ss/top-bottom-split
                                              (object/select-gui (object/create graph-panel))
                                              (object/select-gui (object/create related-panel)))
                                    ;:east (object/select-gui (object/create history-panel))
                                    :west (ss/border-panel
                                            :size [250 :by 100]
                                            :center (object/select-gui
                                                     (object/create project-explorer))
                                            :south (object/select-gui
                                                    (object/create detail-panel))
                                    )))))

