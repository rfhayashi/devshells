(ns devshell.main
  (:require [babashka.cli :as cli]
            [babashka.fs :as fs]))

(def ^:dynamic *working-dir* (fs/cwd))

(defn init [_]
  (let [envrc-path (fs/path *working-dir* ".envrc")]
    (when-not (fs/exists? envrc-path)
      (fs/create-file envrc-path))))

(def commands
  [{:cmds ["init"] :fn init}   ;; devshell init --flake --ignore
   {:cmds ["add"]}    ;; devshell add clojure
   {:cmds ["update"]} ;; devshell update
   ])

(defn -main [& args]
  (cli/dispatch commands args))
