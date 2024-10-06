(ns c3pro-editor.gui
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

;; behaviour definition
;(declare reload-components)
;(declare make-graph-component)
;(declare layout-graph)
;(declare c3pro-graph-to-mxgraph)

;; helpers

;; TODO: refactor common elements


; (load-collaboration "../target/CollaborationBookTripV1.xml" empty-project)

; (add-to-collaboration (add-to-collaboration empty-project "Acquirer") "Traveler")

; (add-to-choreography empty-project "choreo.xml")

;(def tree-model (simple-tree-model map? #(:items %) empty-project))

;(read-config "project.c3pro")

;(write-config "project.c3pro" default-config)

;(def example-project (load-project default-config))

(defn iter-seq
  ([iterable]
    (iter-seq iterable (.iterator iterable)))
  ([iterable i]
    (lazy-seq
      (when (.hasNext i)
        (cons (.next i) (iter-seq iterable i))))))

(defn make-graph-panel-example [f]
  (let [m1            (Message. "book_trip")
        m2            (Message. "check cash")
        traveler      (Role. "Traveler")
        travelAgency  (Role. "TravelAgency")
        i1            (Interaction. "i1" traveler travelAgency m1)
        i2            (Interaction. "i2" travelAgency traveler m2)
        start         (Event. "start")
        end           (Event. "end")
        choreoGraph   (doto (MultiDirectedGraph.)
                        (.addEdge start i1)
                        (.addEdge i1    i2)
                        (.addEdge i2    end))
        mxg           (c3pro-graph-to-mxgraph choreoGraph)
        xml-model     (at.ac.c3pro.io.Bpmn2ChoreographyModel. "../target/BookTripOperation.xml" "BookTripOperation")
        xml-collab    (at.ac.c3pro.io.Bpmn2Collaboration. "../target/CollaborationBookTripV1.xml" "CollaborationBookTrip")
        collab        (.collab xml-collab)
        roles         (.roles collab)
        role-to-pum   (.R2PuM collab)
        xmlChoreoG    (.getdigraph (.choreoModel xml-model))
        acquirer      (first (filter #(= (.name %) "Acquirer") roles))
        acquirer_rpst (.get role-to-pum acquirer)
        acquirer_g    (.getdigraph acquirer_rpst)
        ]
    (do
      (println roles)
      (println acquirer_g)
      (config! (select f [:#graph-panel])
               :items [ ;[(make-graph-component (layout-graph (build-graph))) "span"]
                        ;[(make-graph-component (layout-graph (c3pro-graph-to-mxgraph xmlChoreoG))) "span"]
                        ;[(make-graph-component (layout-graph (c3pro-graph-to-mxgraph acquirer_g))) "span"]
                        ;[(make-graph-component (layout-graph mxg)) "span"]
                      ]))))

; (start-app)
; (kill-frame @a-f)
; (make-graph-panel a-f)
; (make-project-files-panel a-f)
; (add-behaviours a-f)

; (reload-components)

;(print (keys @a-model-cache))

; getting from the model cache
;(@a-model-cache {:model-type :choreography-model, :key "../target/BookTripOperation.xml"})

;(show-events (select f [:#project-files-tree]))

