(ns c3pro-editor.widgets
  (:use [seesaw.core]
        [seesaw.dev]
        [seesaw.mig]
        [seesaw.tree]
        [seesaw.chooser])
  (:import (com.mxgraph.view mxGraph))
  (:import (at.ac.c3pro.node Message Event Interaction)
           (at.ac.c3pro.chormodel MultiDirectedGraph Role ChoreographyModel)
           (at.ac.c3pro.io)
           (org.jbpt.graph Fragment)
           (java.util HashSet)
           (java.awt.event MouseEvent)
           (javax.swing JFileChooser)
           (javax.swing.filechooser FileNameExtensionFilter)))

(native!)

;; widget definitions

(def divider-color "#aaaaaa")

(defn make-frame
  []
  (frame :title "C3Pro"
         :size [800 :by 600]
         :on-close :exit
         :menubar (menubar :items [(menu :text "File" :items [(menu-item :id :menu-file-new-proj)
                                                              (menu-item :id :menu-file-save-proj)
                                                              (menu-item :id :menu-file-load-proj)])
                                   (menu :text "Graph" :items [(menu-item :id :menu-graph-add-choreo)
                                                               (menu-item :id :menu-graph-add-collab)])])
         :content (border-panel
                    ;:border 5
                    ;:hgap 5
                    ;:vgap 5
                    ;:north (label :id :status :text "Ready")
                    :center (top-bottom-split
                              ; top part
                              (scrollable (mig-panel
                                :id :graph-panel
                                :constraints ["" "[grow, fill][grow, fill]" "[grow, fill][grow, fill]"]
                                ;:border (seesaw.border/line-border :bottom 1 :color divider-color)
                                ;; TODO: these items have to be added dynamically
                                :items [ [(label :text "first component")  "span"]
                                         [(label :text "second component") "span"]
                                         [(label :text "third component") "span"]
                                         [(label :text "fourth component") "span"]
                                         [(label :text "Graph / RPST tabs") "dock south"]
                                       ]))
                              (label :text "bottom related part"))
                    :east (table :id :history-panel
                                 :size [150 :by 200]
                                 :model [:columns [:version]
                                         :rows    [{:version "1eab59"}
                                                   {:version "abc3jd"}]
                                        ])
                    :west (border-panel
                            :size [250 :by 100]
                            :center (flow-panel :id :project-explorer-panel
                                                ;:constraints ["" "fill" "fill"]
                                                :size [250 :by 300]
                                                :items []
                                                :border "Project Explorer")
                            :south (mig-panel :id :detail-panel
                                              :constraints ["fillx" "[right]rel[grow, fill]" "[]2[]"]
                                              :items [
                                                       ;[(label :text "ID:") ""    ]
                                                       ;[(label :text "id83x")    "wrap"]
                                                       ;[(label :text "Name:") ""    ]
                                                       ;[(label :text "Start")    "wrap"]
                                                     ]
                                              :border "Node Inspector"
                                              :size [250 :by 400])
                    ))))

(defn save-file []
  (let [ext-filter (FileNameExtensionFilter. "C3Pro Files" (into-array ["c3pro"]))
        fc (JFileChooser.)
        _  (.setFileFilter fc ext-filter)
        retval (.showSaveDialog fc nil)]
    (if (= retval JFileChooser/APPROVE_OPTION)
      (.getSelectedFile fc)
      nil)))

