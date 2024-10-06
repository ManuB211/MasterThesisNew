(ns c3pro-editor.behaviours.menu
  (:require [cbsbot.core :as object]
            [cbsbot.macros :refer [defbehaviour]]
            [seesaw.core :as ss]
            ))

(defn create-menu-item [item]
  (ss/menu-item :id (:item item)
                :action (ss/action :name (:name item)
                                   :handler (fn [e]
                                              (dorun
                                               (map (fn [event]
                                                      (let [obj (object/select-object (:object-keyword item))]
                                                        (object/emit-event event obj e)))
                                                    (:events item)))))))

(defn create-menu [menu]
  (ss/menu :text (:name menu)
           :items (map create-menu-item (:items menu))))

(defn do-initialize-menu [menus]
  (ss/menubar :items (map create-menu menus)))

(defbehaviour initialize-menu
  :listens #{:init}
  :do (fn [this]
        ; TODO actually initialize the menu using (:items this)
        (let [menu-definitions (:items this)
              menu-init (do-initialize-menu menu-definitions)]
          (object/set-attr this :_gui-instance menu-init))))

