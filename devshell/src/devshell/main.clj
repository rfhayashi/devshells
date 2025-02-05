(ns devshell.main
  (:require [babashka.cli :as cli]
            [babashka.fs :as fs]))

(def ^:dynamic *working-dir* (fs/cwd))

(defn init [_]
  (let [envrc-path (fs/path *working-dir* ".envrc")]
    (when-not (fs/exists? envrc-path)
      (fs/write-lines envrc-path ["use flake"]))))

(def commands
  [{:cmds ["init"] :fn init}   ;; devshell init
   {:cmds ["add"]}    ;; devshell add clojure (creates .envrc file if does not exist and add to .gitignore)
   {:cmds ["update"]} ;; devshell update (if directory contains flake updates flake.lock)
   ])

(defn -main [& args]
  (cli/dispatch commands args))
