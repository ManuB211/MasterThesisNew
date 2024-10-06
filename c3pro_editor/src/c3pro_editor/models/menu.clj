(ns c3pro-editor.models.menu
  (:require [cbsbot.macros :refer [defobject]]
            [cbsbot.core :as object]
            [c3pro-editor.gui.menu :refer [menu-gui]]
            [c3pro-editor.object :refer [defmenu]]
            ; required to detect defbahviour definitions
            ;[c3pro-editor.behaviours.menu :as menu-behaviours]
            ))

(defmenu file-menu
  :name "File"
  :items [{:item :new-project
           :object-keyword :project-explorer ; which object is the handler's context?
           :name "New Project"
           :shortcut ""
           :events #{:project.new}}
          {:item :save-project
           :object-keyword :project-explorer
           :name "Save Project"
           :shortcut ""
           :events #{:project.save}}
          {:item :load-project
           :object-keyword :project-explorer
           :name "Load Project"
           :shortcut ""
           :events #{:project.load}}])

(defmenu graph-menu
  :name "Graph"
  :items [{:item :load-collaboration
           :object-keyword :project-explorer
           :name "Load Collaboration"
           :shortcut ""
           :events #{:project.collaboration.load}}
          {:item :load-choreography
           :object-keyword :project-explorer
           :name "Load Choreography"
           :shortcut ""
           :events #{:project.choreography.load}}])

(defmenu propagation-menu
  :name "Change"
  :items [{:item :insert-change
           :object-keyword :propagation-menu
           :name "Insert"
           :shortcut ""
           :events #{:propagation.insert}}
          {:item :delete-change
           :object-keyword :propagation-menu
           :name "Delete"
           :shortcut ""
           :events #{:propagation.delete}}
          {:item :replace-change
           :object-keyword :propagation-menu
           :name "Replace"
           :shortcut ""
           :events #{:propagation.replace}}
          {:item :calculate-and-propagate-change
           :object-keyword :propagation-menu
           :name "Propagate..."
           :shortcut ""
           :events #{:propagation.calculate_and_propagate}}
          ])

(defobject menu
  :tags #{}
  :events #{}
  :behaviours [:initialize-menu]
  :items [file-menu
          graph-menu
          propagation-menu]
  :gui menu-gui)


