(ns c3pro-editor.behaviours.frame
  (:require [cbsbot.core :as object]
            [cbsbot.macros :refer [defbehaviour]]
            [seesaw.core :as ss]
            [c3pro-editor.behaviours.project :refer [load-project']]
            [c3pro-editor.models.project-explorer :refer [tree-model empty-project]]
            ))

(defbehaviour reload-components
  :listens #{:frame.reload}
  :do (fn [this]
        (let [frame     this 
              frame-gui (object/select-gui frame)]
          (do
            (object/emit-event :frame.graph-panel.reload frame)
            (object/emit-event :frame.project-explorer.reload frame)
            ))))

(defbehaviour make-graph-panel
  :listens #{:frame.graph-panel.reload}
  :do (fn [this]
        (ss/config! (ss/select (object/select-gui this) [:#graph-panel])
                    :items []))
        )

(defn render-project-item
  [renderer item]
  (let [text (if (map? (:value item))
               (:text (:value item))
               (.toString (:value item)))
       ]
    (ss/config! renderer :text text)))

; TODO: shouldn't this logic be in project-explorer's behaviour?
(defbehaviour make-project-files-panel
  :listens #{:frame.project-explorer.reload}
  :do (fn [this]
        (let [
              project-explorer (object/select-object :project-explorer)
              config (object/get-attr project-explorer :config)
              root   (object/select-gui (object/select-object :frame))
              empty-tree (tree-model empty-project)
              project-tree (tree-model (load-project'))
              the-tree-model (if (nil? config) empty-tree project-tree)
              project-explorer-panel (ss/scrollable (ss/tree :id :project-files-tree
                                                             :model the-tree-model 
                                                             :renderer render-project-item)
                                                             :size [235 :by 300])
              ]
          (do
            (println "in frame.project-explorer.reload - root" root)
            ; set the project explorer / files tree
            (ss/config! (ss/select root [:#project-explorer-panel])
                        :items [ project-explorer-panel ])
            ; register mouse click event for project file selection
            (ss/listen (ss/select root [:#project-files-tree])
                       :mouse-clicked (fn [e]
                                        (object/emit-event :project.file.selected project-explorer e)))
        ))))

