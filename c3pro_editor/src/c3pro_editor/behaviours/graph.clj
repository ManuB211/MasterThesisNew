(ns c3pro-editor.behaviours.graph
  (:require [cbsbot.core :as object]
            [cbsbot.macros :refer [defbehaviour]]
            [seesaw.core :as ss])
  (import (com.mxgraph.view mxGraph)))

(defn get-node-type [node]
  (cond (instance? at.ac.c3pro.node.AndGateway node)     :and-gateway
        (instance? at.ac.c3pro.node.XorGateway node)     :xor-gateway
        (instance? at.ac.c3pro.node.Interaction node)    :interaction
        (instance? at.ac.c3pro.node.Receive node)        :receive
        (instance? at.ac.c3pro.node.Send node)           :send
        (instance? at.ac.c3pro.node.Event node)          :event
        :else :unknown))

(defbehaviour display-node-inspector
  :listens #{:jgraph.node-selected}
  :do (fn [this cell]
        (let [data (.getValue cell)
              v    (:vertex data)
              node-type (get-node-type v)
              frame-obj (object/select-object :frame)
              frame-gui (object/select-gui frame-obj)
              role      (cond (or (= node-type :receive)
                                  (= node-type :send))
                                (str (first (.getRoles v)))
                              :else nil)
              items     [ [(ss/label :text "ID:") ""]
                          [(ss/label :text (:id data)) "wrap"]
                          [(ss/label :text "Name:") ""]
                          [(ss/label :text (:name data)) "wrap"]
                          [(ss/label :text "Type:") ""]
                          [(ss/label :text (str node-type)) "wrap"]
                        ]
              ; showing the role is confusing, because it is shown in the related graph
              ;items' (if (nil? role)
                       ;items
                       ;(concat items [[(ss/label :text "Role:") ""]
                                      ;[(ss/label :text role) "wrap"]]))
              ]
          (do
            (println "IN display-node-inspector")
            (println "node-type" node-type)
            (println "(type v)" (type v))
            (ss/config! (ss/select frame-gui [:#detail-panel])
                     :items items)))))

(defn inject-selection-jgraph-behaviour
  [selection-event g]
  (do
    (.addListener (.getSelectionModel g)
                  com.mxgraph.util.mxEvent/CHANGE
                  (reify com.mxgraph.util.mxEventSource$mxIEventListener
                    (invoke [this sender evt]
                      (do
                        (println "---")
                        (println sender)
                        (println evt)
                        (println (.getName evt))
                        (println (.getProperties evt))
                        (println "sender.size()" (.size sender))
                        (println "sender.getCell()" (.getCell sender))
                        (println "sender.getCells()" (.getCells sender))
                        (println "sender.getCell().getValue()" (.getValue (.getCell sender)))
                        ;(display-node-inspector (.getCell sender))
                        (object/emit-event selection-event (.getCell sender))
                        ;(.scrollCellToVisible graph-component (.getCell sender) true)
                        ))))
    g))

(defn layout-graph [g]
  (let [l  (com.mxgraph.layout.hierarchical.mxHierarchicalLayout. g javax.swing.SwingConstants/WEST)
        parent (.getDefaultParent g)
        root   (.getCurrentRoot g)
       ]
    (do
      (.setIntraCellSpacing l 100.0)
      (.execute l parent)
      (.setVertexLocation l root 50 50)
      g)))

(defn make-graph-component [graph]
  (com.mxgraph.swing.mxGraphComponent. graph))

(defn hash-vertex [v] (.getId v))

(defn vertices-hashmap [vs] (zipmap (map hash-vertex vs) vs))

(defn c3pro-graph-to-mxgraph [g]
  (let [vs     (.getVertices g)
        es     (.getEdges g)
        ;; define custom graph
        ;; TODO move to custom definition of graph, not inlined like this
        g'     (proxy [com.mxgraph.view.mxGraph] []
                 ;; we always disallow editing of nodes and edges, as well as moving and resizing
                 (isCellLocked [cell]
                   true)
                 ;; we override the cell labeling logic to show the (:name) attribute value
                 (convertValueToString [cell]
                   (when (not (.isEdge cell))
                     (:name (.getValue cell)))))
        user-object (fn [v]
                      {:name   (.getName v)
                       :id     (.getId   v)
                       :vertex v})
        parent (.getDefaultParent g')
        vs-map (vertices-hashmap vs)
        vs'    (for [[id v] vs-map]
                 [id (.insertVertex g' parent nil (user-object v) 20 20 80 30)])
        vs-map' (apply hash-map (apply concat vs'))
        es'    (try
                 (do
                   (.beginUpdate (.getModel g'))
                   (doall (for [e es]
                         (let [source-v (hash-vertex (.getSource e))
                               target-v (hash-vertex (.getTarget e))
                               v1       (vs-map' source-v)
                               v2       (vs-map' target-v)
                               e        (.insertEdge g' parent nil "" v1 v2)]
                           (do
                             (println "adding edge: " e)
                             e)))))
                 (finally
                   (.endUpdate (.getModel g'))))
       ]
      g'))

(defn set-public-model-cache [mc k v]
  (assoc mc {:model-type :public-model :key k} v))

(defn set-choreography-model-cache [mc k v]
  (assoc mc {:model-type :choreography-model :key k} v))

(defn digraph-to-mx-graph-component [selection-event digraph]
  (let [selection-handler' (partial inject-selection-jgraph-behaviour selection-event)]
    (-> digraph
        c3pro-graph-to-mxgraph
        layout-graph
        selection-handler'
        make-graph-component)))

(defn get-jgraph-vertices
  "return an array of cells of the graph in the graph component"
  [graph-component]
  (let [graph    (.getGraph graph-component)
        parent   (.getDefaultParent graph)]
    (.getChildVertices graph parent)))

(defbehaviour render-graph
  :listens #{:graph.render-graph}
  :do (fn [this digraph]
        (let [frame-obj (object/select-object :frame)
              frame-gui (object/select-gui frame-obj)
              component (atom (digraph-to-mx-graph-component :jgraph.node-selected digraph))
              ]
          (do
            (object/set-attr (object/select-object :graph-panel) :graph-component component)
            (object/set-attr (object/select-object :graph-panel) :c3pro-graph digraph)
            (ss/config! (ss/select frame-gui [:#graph-panel])
                            :items [ @component ])
            ; NOTE: select a random cell to focus on when opening (remove this later)
            (let [vertices (get-jgraph-vertices @component)
                  random-cell (rand-nth vertices)]
              (do
                (println "listing all graph vertices" vertices (.getValue random-cell))
                ; TODO compared to clicking, this won't don't center the selected cell
                ;(.addCell (.getSelectionModel (.getGraph @component)) random-cell)
                ;(.scrollCellToVisible @component random-cell true)
                ))))))

(defn find-equivalent-cell [graph-component node graph1 graph2]
  (let [vertices (get-jgraph-vertices graph-component)
        filtered (filter (fn [x]
                           (let [v  (:vertex (.getValue x))
                                 g1 graph1
                                 g2 graph2
                                ]
                             (do
                               (println "---- Comparing v vs node" v node)
                               (at.ac.c3pro.util.FragmentUtil/matchingPaths g1 v g2 node))))
                         vertices)]
    (first filtered)))
