(defproject jepsen.clj-stm "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[org.clojure/clojure "1.10.3"]
                 [jepsen "0.2.7"]]
  :main jepsen.clj-stm
  :jvm-opts ["-Djava.awt.headless=true"]
  :repl-options {:init-ns jepsen.clj-stm})
