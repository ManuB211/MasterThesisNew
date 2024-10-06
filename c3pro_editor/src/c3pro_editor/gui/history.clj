(ns c3pro-editor.gui.history
  (require [cbsbot.core :as object]
           [cbsbot.macros :refer [defgui]]
           [seesaw.core :as ss]))

(defgui history-panel-gui
        :init (fn []
                (ss/vertical-panel
                  :items [(ss/table :id :history-panel
                                    :size [150 :by 200]
                                    :model [:columns [:version]
                                            :rows    [{:version "1eab59"}
                                                      {:version "abc3jd"}]
                                           ])]
                  :border "Version History")))
