(ns c3pro-editor.guijgraphtest
  (:use [seesaw.core]
        [seesaw.dev]
        [seesaw.mig])
  (:import (com.mxgraph.view mxGraph)))

(def myframe (frame :title "Hello"
                    :on-close :exit
                    :size [800 :by 600]))

(def mygraph (mxGraph.))

(defn display [content]
  (config! myframe :content content)
  content)

(-> myframe pack! show!)

(display (scrollable (com.mxgraph.swing.mxGraphComponent. mygraph)))

(def parent (.getDefaultParent mygraph))

(def v1 (.insertVertex mygraph parent nil "hello!" 20 20 80 30))

(def v2 (.insertVertex mygraph parent nil "world!" 240 150 80 30))

(.insertEdge mygraph parent nil "Edge" v1 v2)

(show-options myframe)
