(ns jepsen.clj-stm
  (:require
    [jepsen
     [cli :as cli]]
    [jepsen.clj-stm.cas :as cas]
    [jepsen.clj-stm.list-append :as la]))


(defn -main
  [& args]
  (cli/run! (merge
              ;; (cli/single-test-cmd {:test-fn cas/test-fn})
              (cli/single-test-cmd {:test-fn la/test-fn})
              (cli/serve-cmd))
            args))
