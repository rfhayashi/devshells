(ns devshell.main
  (:refer-clojure :exclude [update])
  (:require [babashka.cli :as cli]
            [babashka.fs :as fs]
            [babashka.process :refer [shell]]
            [cheshire.core :as json]
            [clojure.string :as str]
            [clojure.java.io :as io]))

(def ^:dynamic *working-dir* (fs/cwd))

(def devshell-owner "rfhayashi")
(def devshell-repo "devshells")

(defn devshell-revision []
  (-> (shell {:out :string} (format "gh api /repos/%s/%s/commits/main" devshell-owner devshell-repo))
      :out
      (json/parse-string keyword)
      :sha))

(def flake-base-url (format "github:%s/%s" devshell-owner devshell-repo))

(defn flake-url []
  (format "%s/%s" flake-base-url (devshell-revision)))

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

(def devshell-rx (re-pattern (format "(%s)/[^?]*" flake-base-url)))

(defn direnv-update [_]
  (with-path [envrc-path ".envrc"]
    (fs/update-file (str envrc-path) str/replace devshell-rx (format "$1/%s" (devshell-revision))))
  (shell "direnv allow"))

(defn direnv-ignore [_]
  (with-path [git-path ".git"]
    (when (fs/exists? git-path)
      (fs/write-lines (fs/path git-path "info/exclude") [".direnv" ".envrc"] {:append true}))))

(def direnv-commands
  [{:cmds ["add"] :fn direnv-add :args->opts [:template]}
   {:cmds ["update"] :fn direnv-update}
   {:cmds ["ignore"] :fn direnv-ignore}])

(defn direnv [{:keys [args]}]
  (cli/dispatch direnv-commands args))

(def commands
  [{:cmds ["init"] :fn init :args->opts [:template]}
   {:cmds ["direnv"] :fn direnv}
   {:cmds ["update"] :fn update}
   ])

(defn -main [& args]
  (cli/dispatch commands args))
