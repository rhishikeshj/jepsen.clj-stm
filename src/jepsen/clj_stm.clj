(ns jepsen.clj-stm
  (:require
    [clojure.string :as str]
    [clojure.tools.logging :refer :all]
    [jepsen
     [checker :as checker]
     [cli :as cli]
     [client :as client]
     [control :as c]
     [db :as db]
     [tests :as tests]
     [generator :as gen]]
    [jepsen.control.util :as cu]
    [jepsen.os.debian :as debian]
    [jepsen.tests :as tests]
    [knossos.model :as model])
  (:import
    (knossos.model
      Model)))


;; DB ;;

(def logdir "/opt/stm")
(def logfile (str logdir "/stm.log"))


(defn db
  "Etcd DB for a particular version."
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


;; Client ;;

(defn r
  [_ _]
  {:type :invoke, :f :read, :value nil})


(defn w
  [_ _]
  {:type :invoke, :f :write, :value (rand-int 5)})


(defn cas
  [_ _]
  {:type :invoke, :f :cas, :value [(rand-int 5) (rand-int 5)]})


(def state {:foo (ref 0)})


(defrecord Client
  [conn]

  client/Client

  (open!
    [this test node]
    this)


  (setup! [this test])


  (invoke!
    [this test op]
    (case (:f op)
      :read (assoc op
                   :type :ok
                   :value @(:foo state))
      :write (do (dosync
                   (ref-set (:foo state) (:value op)))
                 (assoc op :type :ok))
      :cas (let [[old new] (:value op)]
             (if (= (:foo (:conn this)) old)
               (do
                 (dosync (ref-set (:foo state) new))
                 (assoc op :type :ok))
               (assoc op :type :fail)))))


  (teardown! [this test])


  (close! [_ test]))


(defn stm-test
  [opts]
  (merge tests/noop-test
         opts
         {:name "clj-stm"
          :os debian/os
          :db (db "fake-version")
          :client (Client. nil)
          :pure-generators true
          :generator (->> (gen/mix [r w cas])
                          (gen/stagger 1)
                          (gen/nemesis nil)
                          (gen/time-limit 15))
          :checker (checker/linearizable
                     {:model     (model/cas-register)
                      :algorithm :linear})}))


(defn -main
  [& args]
  (cli/run! (merge (cli/single-test-cmd {:test-fn stm-test})
                   (cli/serve-cmd))
            args))
