(ns devshell.main
  (:require [babashka.cli :as cli]
            [babashka.fs :as fs]
            [babashka.process :refer [shell]]
            [cheshire.core :as json]))

(def ^:dynamic *working-dir* (fs/cwd))

(defn devshell-revision []
  (-> (shell {:out :string} "gh api /repos/rfhayashi/devshells/commits/main")
      :out
      (json/parse-string keyword)
      :sha))

(defn template-url [template]
  (format "github:rfhayashi/devshells/%s#%s" (devshell-revision) template))

(defn init [{{:keys [template]} :opts}]
  (shell "nix flake init -t" (template-url template))
  (let [envrc-path (fs/path *working-dir* ".envrc")]
    (when-not (fs/exists? envrc-path)
      (fs/write-lines envrc-path ["use flake"])))
  (shell "direnv allow"))

(def direnv-commands
  [{:cmds ["add"]} ;; add a devshell template to .envrc
   {:cmds ["update"]} ;; update revisions
   {:cmds ["ignore"]} ;; adds direnv files to private git ignore
   ])

(def commands
  [{:cmds ["init"] :fn init :args->opts [:template]}   ;; devshell init clojure
   {:cmds ["direnv"]}  
   {:cmds ["update"]} ;; devshell update (if directory contains flake updates flake.lock)
   ])

(defn -main [& args]
  (cli/dispatch commands args))
