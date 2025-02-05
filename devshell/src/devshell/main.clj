(ns devshell.main
  (:refer-clojure :exclude [update])
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

(defmacro when-file-does-not-exist [[binding path] & body]
  `(let [~binding (fs/path *working-dir* ~path)]
     (when-not (fs/exists? ~binding)
       ~@body)))

(defn init [{{:keys [template]} :opts}]
  (shell "nix flake init -t" (template-url template))
  (when-file-does-not-exist [envrc-path ".envrc"]
    (fs/write-lines envrc-path ["use flake"]))
  (shell "direnv allow"))

(defn update [_]
  (shell "nix flake update"))

(def direnv-commands
  [{:cmds ["add"]}    ;; add a devshell template to .envrc
   {:cmds ["update"]} ;; update revisions
   {:cmds ["ignore"]} ;; adds direnv files to private git ignore
   ])

(def commands
  [{:cmds ["init"] :fn init :args->opts [:template]}
   {:cmds ["direnv"]}  
   {:cmds ["update"] :fn update}
   ])

(defn -main [& args]
  (cli/dispatch commands args))
