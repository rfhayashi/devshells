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

(def init-command
  {:fn init
   :usage "devshell init <template>"
   :description "Creates the files for the given <template>."
   :opts {:args->opts [:template]
          :require [:template]}})

(defn update [_]
  (shell "nix flake update"))

(def update-command
  {:fn update
   :usage "devshell update"
   :description "Updates flake.lock"})

(defn direnv-add [{{:keys [template]} :opts}]
  (when-file-does-not-exist [envrc-path ".envrc"]
    (fs/create-file envrc-path))
  (with-path [envrc-path ".envrc"]
    (fs/write-lines envrc-path [(format "use flake \"%s\"" (devshell-url template))] {:append true}))
  (shell "direnv allow"))

(def direnv-add-command
  {:fn direnv-add
   :usage "devshell direnv add <template>"
   :description "Adds the <template> to .envrc file"
   :opts {:args->opts [:template]
          :require [:template]}})

(def devshell-rx (re-pattern (format "(%s)/[^?]*" flake-base-url)))

(defn direnv-update [_]
  (with-path [envrc-path ".envrc"]
    (fs/update-file (str envrc-path) str/replace devshell-rx (format "$1/%s" (devshell-revision))))
  (shell "direnv allow"))

(def direnv-update-command
  {:fn direnv-update
   :usage "devshell direnv update"
   :description "Update the .envrc file to point to templates latest versions"})

(defn direnv-ignore [_]
  (with-path [git-path ".git"]
    (when (fs/exists? git-path)
      (fs/write-lines (fs/path git-path "info/exclude") [".direnv" ".envrc"] {:append true}))))

(def direnv-ignore-command
  {:fn direnv-ignore
   :usage "devshell direnv ignore"
   :description "Adds direnv files to private git ignore"})

(defn print-help [{:keys [usage description opts]}]
  (println (format "usage: %s" usage))
  (println)
  (println description)
  (when (:spec opts)
    (println (cli/format-opts opts))))

(def commands
  [{:cmds ["init"] :command init-command}
   {:cmds ["update"] :command update-command}
   {:cmds ["direnv" "add"] :command direnv-add-command}
   {:cmds ["direnv" "update"] :command direnv-update-command}
   {:cmds ["direnv" "ignore"] :command direnv-ignore-command}])

;; TODO list commands in help
(defn help-table [commands]
  (concat
   [{:cmds [] :fn (fn [_] (print-help {:usage "devshell <command>" :description "Cli utility to manage devshells"}))}]
   (map
    (fn [{:keys [cmds command]}]
      {:cmds cmds
       :fn (fn [{{:keys [help]} :opts}]
             (when help
               (print-help command)))
       :aliases {:h :help}})
    commands)))

(defn command-table [commands]
  (map
   (fn [{:keys [cmds] {:keys [fn opts]} :command}]
     (assoc opts :cmds cmds :fn fn))
   commands))

(defn command-error-fn [{:keys [cause wrong-input msg]}]
  (if msg
    (println msg)
    (case cause
      :no-match (println (format "No such command: %s" wrong-input))))
  (System/exit 1))

;; TODO handle exceptions (only returning exit code) and add verbose mode
;; when one wants to see the exception
(defn -main [& args]
  (if (-> (cli/parse-args args {:aliases {:h :help}}) :opts :help)
    (cli/dispatch (help-table commands) args {:error-fn command-error-fn})
    (cli/dispatch (command-table commands) args {:error-fn command-error-fn})))
