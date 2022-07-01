(ns jepsen.clj-stm.db
  (:require
    [clojure.tools.logging :refer :all]
    [jepsen [db :as db]]))


;; DB ;;

(def logdir "/opt/stm")
(def logfile (str logdir "/stm.log"))


(defn db
  "Fake DB"
  [version]
  (reify db/DB
    (setup!
      [_ test node]
      (info node "nothing to install with clj stm" version))

    (teardown!
      [_ test node]
      (info node "nothing to tear down with clj stm"))


    db/LogFiles

    (log-files
      [_ test node]
      [logfile])))
