(ns c3pro-editor.object)

(defmacro defmenu [name & keyvals]
  (let [tpl (apply hash-map keyvals)]
    `(def ~name ~tpl)))

(defn menu-item [& keyvals] nil)

