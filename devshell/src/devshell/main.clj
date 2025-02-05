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

(defn flake-url []
  (format "github:rfhayashi/devshells/%s" (devshell-revision)))

(defn template-url [template]
  (format "%s#%s" (flake-url) template))

(defn devshell-url [template]
  (format "%s?dir=%s" (flake-url) template))

(defmacro with-path [[binding local-path] & body]
  `(let [~binding (fs/path *working-dir* ~local-path)]
     ~@body))

(defmacro when-file-does-not-exist [[binding path] & body]
  `(with-path [~binding ~path]
     (when-not (fs/exists? ~binding)
       ~@body)))

(defn init [{{:keys [template]} :opts}]
  (shell "nix flake init -t" (template-url template))
  (when-file-does-not-exist [envrc-path ".envrc"]
    (fs/write-lines envrc-path ["use flake"]))
  (shell "direnv allow"))

(defn update [_]
  (shell "nix flake update"))

(defn direnv-add [{{:keys [template]} :opts}]
  (when-file-does-not-exist [envrc-path ".envrc"]
    (fs/create-file envrc-path))
  (with-path [envrc-path ".envrc"]
    (fs/write-lines envrc-path [(format "use flake \"%s\"" (devshell-url template))] {:append true}))
  (shell "direnv allow"))

(def direnv-commands
  [{:cmds ["add"] :fn direnv-add :args->opts [:template]}
   {:cmds ["update"]} ;; update revisions
   {:cmds ["ignore"]} ;; adds direnv files to private git ignore
   ])

(defn direnv [{:keys [args]}]
  (cli/dispatch direnv-commands args))

(def commands
  [{:cmds ["init"] :fn init :args->opts [:template]}
   {:cmds ["direnv"] :fn direnv}
   {:cmds ["update"] :fn update}
   ])

(defn -main [& args]
  (cli/dispatch commands args))
