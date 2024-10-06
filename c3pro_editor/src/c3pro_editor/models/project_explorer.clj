(ns c3pro-editor.models.project-explorer
  (require [cbsbot.macros :refer [defobject]]
           [cbsbot.core :as object]
           [c3pro-editor.gui.project-explorer :refer [project-explorer-gui]]
           [c3pro-editor.config :as config]
           ; this import is required to load and register them
           ;[c3pro-editor.behaviours.project :as project-behaviours]
           )
  (use [seesaw.tree]))

(defobject project-explorer
  :tags #{:tree :project :config}
  :events #{:c3pro.model-selected}
  :behaviours [:render-project-tree
               :project-config-changed
               :project-explorer-file-selected
               ; menu stuff
               ; file
               :new-project
               :save-project
               :load-project
               ; graph
               :load-collaboration
               :load-choreography]
  :singleton true
  :config (config/->C3ProConfig nil nil)
  :model-cache {}
  ; caches model-key -> Model instance,
  ; where model-key is either
  ;
  ;     {:model-type :public-model :key X}
  ;
  ; or
  ;
  ;     {:model-type :choreography-model :key X}
  :current-model nil
  ; reference to the current at.ac.c3pro.chormodel.Collaboration instance
  :collaboration-instance nil
  :gui project-explorer-gui)

(def empty-project {:text "Project"
                    :items [{:text "Choreography"
                             :items []}
                            {:text "Collaboration"
                             :items []}
                           ]
                   })

(defn tree-model [project]
  (simple-tree-model map? #(:items %) project))

