(ns c3pro-editor.behaviours.project
  (require [cbsbot.macros :refer [defbehaviour]]
           [cbsbot.core :as object]
           [seesaw.core :as ss]
           [seesaw.chooser :as sschooser]
           [c3pro-editor.config :refer [write-config read-config ->C3ProConfig]]
           [c3pro-editor.widgets :as widgets]
           [c3pro-editor.utils :as utils]
           [c3pro-editor.behaviours.graph :as graph]
           [c3pro-editor.models.project-explorer :refer [empty-project]]
           )
  (import (at.ac.c3pro.chormodel Role)))

(defbehaviour new-project
  :listens #{:project.new}
  :do (fn [this e]
        ; event emitted by menu mouse click
        ; TODO check for unsaved changes to be persisted?
        (println "handling project.new!")
        (object/set-attr this :config (->C3ProConfig nil nil))
        (object/emit-event :frame.reload (object/select-object :frame))
        ))

; TODO re-save to last filename?
(defbehaviour save-project
  :listens #{:project.save}
  :do (fn [this e]
        (let [filename (.getAbsolutePath (widgets/save-file))
              config   (object/get-attr this :config)]
          (when (not (nil? config))
            (println "Saving project to " filename)
            (try
              (write-config filename config)
              (catch Exception e
                (ss/alert (format "Could not save project to file: %s" filename))))))))

(defbehaviour load-project
  :listens #{:project.load}
  :do (fn [this e]
        (do
          (println "Loading project...")
          (let [f (sschooser/choose-file :filters [["C3Pro" ["c3pro"]
                                         (sschooser/file-filter "All files" (constantly true))]])
                config-filename (.getAbsolutePath f)
                config          (try (read-config config-filename)
                                     (catch Exception e (do
                                                          (ss/alert (format "Could not load project: %s" config-filename))
                                                          nil)))
                ]
            (when config
              (object/set-attr this :config config)
              (object/emit-event :frame.reload (object/select-object :frame))
              )))))

(defbehaviour load-collaboration
  :listens #{:project.collaboration.load}
  :do (fn [this e]
        ; 1 - overwrite the collaboration attribute of a-config - handle nil case too
        ; 2 - reload all the components
        (let [filename   (.getAbsolutePath (sschooser/choose-file :filters [["XML" ["xml"]
                                                             (sschooser/file-filter "All files" (constantly true))]]))
              config     (object/get-attr this :config)
              config'    (assoc config :collaboration-filename filename)
             ]
          (do
            (println "at behaviour load-collaboration")
            (println "config" config)
            (println "filename" filename)
            (println "config'" config')
            (object/set-attr this :config config')
            (object/emit-event :project.config.changed this config' :collaboration-filename)))))

(defbehaviour load-choreography
  :listens #{:project.choreography.load}
  :do (fn [this e]
        (let [filename (.getAbsolutePath (sschooser/choose-file :filters [["XML" ["xml"]
                                                                           (sschooser/file-filter "All files" (constantly true))]]))
              config   (object/get-attr this :config)
              config'  (assoc config :choreography-filename filename)]
            (do
              (object/set-attr this :config config')
              (object/emit-event :project.config.changed this config' :choreography-filename)))))

(defbehaviour project-config-changed
  :listens #{:project.config.changed}
  :do (fn [this new-config filename-keyword]
        (do
          (println "at project-config-changed")
          (println "new-config" new-config)
          (println "filename-keyword" filename-keyword)
        (let [filename (get new-config filename-keyword)
              the-type (cond (= filename-keyword :collaboration-filename) "collaboration"
                             :else "choreography")]
          (do
            (println "filename" filename)
            (println "the-type" the-type)
          (try
            (object/emit-event :frame.reload (object/select-object :frame))
            (catch Exception e
              (ss/alert (str "Could not load " the-type ": " filename)))))))))

(defbehaviour project-explorer-file-selected
  :listens #{:project.file.selected}
  :do (fn [this e]
        (when (utils/double-click? e)
          (let [selected-item  (last (ss/selection e))
                model-key      (cond (= (type selected-item) Role) {:model-type :public-model :key selected-item}
                                     :else {:model-type :choreography-model :key selected-item})
                project-obj    (object/select-object :project-explorer)
                model-cache    (object/get-attr project-obj :model-cache)
                model-instance (model-cache model-key)
                digraph        (.getdigraph model-instance)
               ]
            (do
              (object/set-attr this :current-model model-instance)
              (object/emit-event :graph.render-graph (object/select-object :graph-panel) digraph)
            )))))

;; TODO: refactor common elements
(defn add-to-collaboration [{_ :text items :items :as project} x]
  (let [{items' :items :as collab} (first (filter #(= (:text %) "Collaboration") items))
        other-items                (filter #(not (= (:text %) "Collaboration")) items)
        changed-collab             (assoc collab :items (conj items' x))
        new-proj-items             (conj other-items changed-collab)
       ]
  (assoc project :items new-proj-items)))

;; TODO: refactor common elements
(defn add-to-choreography [{_ :text items :items :as project} x]
  (let [{items' :items :as choreo} (first (filter #(= (:text %) "Choreography") items))
        other-items                (filter #(not (= (:text %) "Choreography")) items)
        changed-choreo             (assoc choreo :items (conj items' x))
        new-proj-items             (conj other-items changed-choreo)
       ]
  (assoc project :items new-proj-items)))

;; TODO: handle loading exceptions
(defn load-collaboration' [filename tree-model]
  (let [ xml-collab    (at.ac.c3pro.io.Bpmn2Collaboration. filename "")
        collab        (.collab xml-collab)
        roles         (.roles collab)
        role-to-pum   (.R2PuM collab)
        project-obj   (object/select-object :project-explorer)
        model-cache   (object/get-attr project-obj :model-cache)]
    (do
      (object/set-attr project-obj :collaboration-instance collab)
      (reduce (fn [xs role] (let [mc (object/get-attr project-obj :model-cache)]
                              (do
                                (object/set-attr project-obj :model-cache (graph/set-public-model-cache mc role (.get role-to-pum role)))
                                (add-to-collaboration xs role))))
              tree-model roles))
    ))

(defn load-choreography' [filename tree-model]
  (let [
        xml-model   (at.ac.c3pro.io.Bpmn2ChoreographyModel. filename "")
        xml-choreo  (.choreoModel xml-model)
        project-obj   (object/select-object :project-explorer)
        model-cache (object/get-attr project-obj :model-cache)
        ]
    (do
      (println "load-choreography' - filename" filename)
      (object/set-attr project-obj :model-cache (graph/set-choreography-model-cache model-cache filename xml-choreo))
      (add-to-choreography tree-model filename)
      )))

(defn load-project' []
  (let [config          (object/get-attr (object/select-object :project-explorer) :config)
        collab-filename (:collaboration-filename config)
        choreo-filename (:choreography-filename config)]
    (do
      (println "load-project' - config" config)
      (println "load-project' - collab-filename" collab-filename)
      (println "load-project' - choreo-filename" choreo-filename)
  (cond (and (nil? collab-filename) (nil? choreo-filename)) empty-project
        (nil? collab-filename) (load-choreography' choreo-filename empty-project)
        (nil? choreo-filename) (load-collaboration' collab-filename empty-project)
        :else (load-collaboration' collab-filename
                                  (load-choreography' choreo-filename empty-project)))
    )))

