(ns jepsen.clj-stm.cas
  (:require
    [jepsen
     [checker :as checker]
     [generator :as gen]
     [client :as client]]
    [jepsen.clj-stm.db :as db]
    [jepsen.os.debian :as debian]
    [jepsen.tests :as tests]
    [knossos.model :as model])
  (:import
    (knossos.model
      Model)))


;; CAS Client ;;

(defn r
  [_ _]
  {:type :invoke, :f :read, :value nil})


(defn w
  [_ _]
  {:type :invoke, :f :write, :value (rand-int 5)})


(defn cas
  [_ _]
  {:type :invoke, :f :cas, :value [(rand-int 5) (rand-int 5)]})


(def cas-state {:foo (ref 0)})


(defrecord CASClient
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
                   :value @(:foo cas-state))
      :write (do (dosync
                   (ref-set (:foo cas-state) (:value op)))
                 (assoc op :type :ok))
      :cas (let [[old new] (:value op)]
             (if (= (:foo (:conn this)) old)
               (do
                 (dosync (ref-set (:foo cas-state) new))
                 (assoc op :type :ok))
               (assoc op :type :fail)))))


  (teardown! [this test])


  (close! [_ test]))


(defn test-fn
  [opts]
  (merge tests/noop-test
         opts
         {:name "clj-stm"
          :os debian/os
          :db (db/db "fake-version")
          :client (CASClient. nil)
          :pure-generators true
          :generator (->> (gen/mix [r w cas])
                          (gen/stagger 1)
                          (gen/nemesis nil)
                          (gen/time-limit 15))
          :checker (checker/linearizable
                     {:model     (model/cas-register)
                      :algorithm :linear})}))
