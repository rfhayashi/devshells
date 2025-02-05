(ns devshell.init-test
  (:require [clojure.test :refer [deftest is testing]]
            [babashka.fs :as fs :refer [with-temp-dir]]
            [devshell.main :refer [*working-dir* -main]]))

(defmacro with-working-dir [[binding] & body]
  `(with-temp-dir [~binding]
     (binding [*working-dir* ~binding]
       ~@body)))

(deftest init-test
  (testing "creates .envrc"
    (with-working-dir [dir]
      (-main "init") 
      (is (= ["use flake"] (fs/read-all-lines (fs/path dir ".envrc"))))))
  (testing "does not clobber .envrc"
    (with-working-dir [dir]
      (fs/write-lines (fs/path dir ".envrc") ["some content"])
      (-main "init")
      (is (= ["some content"] (fs/read-all-lines (fs/path dir ".envrc"))))))
  (testing "creates flake.nix")
  (testing "does not clobber flake.nix")
  (testing "adds .direnv to .gitignore")
  (testing "does not clobber .gitignore"))
