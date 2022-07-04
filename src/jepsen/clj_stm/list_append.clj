(ns jepsen.clj-stm.list-append
  (:require
    [clojure.string :as str]
    [clojure.tools.logging :refer :all]
    [elle.list-append :as elle.list-append]
    [jepsen
     [checker :as checker]
     [cli :as cli]
     [client :as client]
     [control :as c]
     [tests :as tests]
     [generator :as gen]]
    [jepsen.clj-stm.db :as db]
    [jepsen.control.util :as cu]
    [jepsen.os.debian :as debian]
    [jepsen.tests :as tests]
    [jepsen.tests.cycle.append :as list-append]
    [knossos.model :as model])
  (:import
    (knossos.model
      Model)))


;; list-append client


(def la-state (atom {}))


(defn run-txn
  [[op k v]]
  (case op
    :r [:r k (if-let [v' (get @la-state k)]
               @v'
               nil)]
    :append (do
              (swap! la-state
                     (fn [m]
                       (dosync (update m
                                       k
                                       #(if %
                                          (ref (alter % conj v))
                                          (ref [v]))))))
              [op k v])))


(defrecord LAClient
  [conn]

  client/Client

  (open!
    [this test node]
    this)


  (setup! [this test])


  (invoke!
    [this test op]
    (let [txns (:value op)
          txns' (vec (map run-txn txns))]
      (assoc op :type :ok :value txns')))


  (teardown! [this test])


  (close! [_ test]))


;; list-append workload

(defn workload
  "A generator, client, and checker for a list-append test."
  [opts]
  (-> (list-append/test {:key-count          10
                         :key-dist           :exponential
                         ;; :key-dist           :uniform
                         :max-txn-length     (:max-txn-length opts 4)
                         :max-writes-per-key (:max-writes-per-key opts 4)
                         :consistency-models [:strong-snapshot-isolation]
                         :cycle-search-timeout 30000})
      (assoc :client (LAClient. nil))
      (update :checker (fn [c]
                         (checker/compose
                           {:elle c})))))


(defn test-fn
  [_]
  (let [opts {:max-writes-per-key 4
              :rate 10
              :time-limit 10}
        workload (workload opts)]
    (merge tests/noop-test
           opts
           {:name "clj-stm"
            :os debian/os
            :db (db/db "fake-version")
            :client (:client workload)
            :pure-generators true
            :generator (gen/phases
                         (->> (:generator workload)
                              (gen/nemesis nil)
                              (gen/stagger (/ (:rate opts)))
                              (gen/time-limit (:time-limit opts))))
            :checker (:checker workload)})))
