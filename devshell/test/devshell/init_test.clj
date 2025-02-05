(ns devshell.init-test
  (:require [clojure.test :refer [deftest is testing]]
            [babashka.fs :as fs :refer [with-temp-dir]]
            [devshell.main :refer [*working-dir* -main]]))

(defmacro with-working-dir [[binding] & body]
  `(with-temp-dir [~binding]
     (binding [*working-dir* ~binding]
       ~@body)))

(deftest init-test
  (testing "creates .envrc file"
    (with-working-dir [dir]
      (-main "init") 
      (is (= "" (slurp (str (fs/path dir ".envrc"))))))))

