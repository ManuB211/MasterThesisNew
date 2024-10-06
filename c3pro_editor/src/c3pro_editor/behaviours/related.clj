(ns c3pro-editor.behaviours.related
  (:require [cbsbot.core :as object]
            [cbsbot.macros :refer [defbehaviour]]
            [c3pro-editor.behaviours.graph :as graph]
            [seesaw.core :as ss]))

(defbehaviour display-related-nodes
  :listens #{:jgraph.node-selected}
  :do (fn [this cell]
        (let [data           (.getValue cell)
              v              (:vertex data)
              model-instance (object/get-attr (object/select-object :project-explorer) :current-model)
              collab         (object/get-attr (object/select-object :project-explorer) :collaboration-instance)
              main-digraph   (.getdigraph model-instance)
              frame-obj      (object/select-object :frame)
              frame-gui      (object/select-gui frame-obj)
              roles          (.getRoles v)
              msg            (.getMessage v)
              ; TODO: this is a temporary log output for screenshotting
              bogus-widget   (ss/vertical-panel
                               :size [800 :by 400]
                               :items [(ss/scrollable (ss/text :text (slurp "propagation.txt")
                                      :multi-line? true
                                      :editable? false
                                      :rows 15
                                      :size [800 :by 200]))
                               (ss/scrollable (ss/text :text (slurp "mapping.txt")
                                       :multi-line? true
                                       :editable? false
                                       :rows 15
                                       :size [800 :by 200]))
                                      ])
              find-and-scroll-to-related-node (fn [related-node role]
                                                (let [ ; TODO: duplicate from project-explorer-file-selected
                                                       model-key {:model-type :public-model :key role}
                                                       model-cache (object/get-attr (object/select-object :project-explorer) :model-cache)
                                                       role-model (model-cache model-key)
                                                       digraph (.getdigraph role-model)
                                                       graph-component (graph/digraph-to-mx-graph-component :related.jgraph.node-selected digraph)
                                                       widget (ss/vertical-panel
                                                                :size [400 :by 400]
                                                                :items [(ss/label :text (str "ID: " (.getId related-node)))
                                                                        (ss/label :text (str "Name: " related-node))
                                                                        (ss/label :text (str "Type: " (graph/get-node-type related-node)))
                                                                        (ss/label :text (str "Role: " (str role)))
                                                                        graph-component])
                                                       frame-gui (object/select-gui (object/select-object :frame))]
                                                    ; Because the cell does not exist in this graph, we need to find the actual cell where the cell is equal.
                                                    (let [cell' (graph/find-equivalent-cell graph-component related-node digraph main-digraph)]
                                                      (do
                                                        (println "found equivalent cell: " cell' (.getValue cell'))
                                                        ; this one returns stack overflow because the events are run recursively
                                                        (.addCell (.getSelectionModel (.getGraph graph-component)) cell')
                                                        (.scrollCellToVisible graph-component cell')
                                                                          (ss/config! (ss/select frame-gui [:#related-panel]) :items [widget bogus-widget])))))
              ]
          (do
            (println "IN DISPLAY-RELATED-NODES")
            (println "---- vertex:" v)
            (println "---- model:" model-instance)
            (cond (instance? at.ac.c3pro.node.Interaction v) (println "handling interaction")
                  (instance? at.ac.c3pro.node.Receive v)     (find-and-scroll-to-related-node (.get (.Pu2Pu collab) v) (first roles))
                  (instance? at.ac.c3pro.node.Send v)        (find-and-scroll-to-related-node (.get (.Pu2Pu collab) v) (first roles))
                  :else (println "not handling" v))
            (println "---- roles:" roles)
            (println "---- collaboration:" collab)
            (println "---- collaboration hashmap:" (.Pu2Pu collab))))))
