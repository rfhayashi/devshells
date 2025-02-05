(ns devshell.main)

(def commands
  [{:cmds ["init"]} ;; devshell init --flake --ignore
   {:cmds ["add"]} ;; devshell add clojure
   {:cmds ["update"]} ;; devshell update
   ])

(defn -main [& _]
  (prn "hello"))
