(ns event-gateway.core-test-with-jms
  (:require [clojure.test :refer :all]
            [clojure.pprint :refer :all]
            [event-gateway.core :refer :all])
  (:use clj-assorted-utils.util
        clj-jms-activemq-toolkit.jms))

(def local-jms-server "tcp://localhost:42424")
(def local-gw-jms-server "tcp://localhost:41414")

(def jms-test-adapter-cfg
  {"jms-test-adapter"
    {"in-url" local-jms-server
     "out-url" local-jms-server
     "ks" nil
     "ts" nil
     "rules" {"a-b" {"in-topic" "/topic/a"
                     "out-topic" "/topic/b"
                     "operation" {"name" "no-op"
                                  "parameters" nil}}}}})

(def jms-test-gw-cfg
  {"default-gw" {"gw-jms-url" local-gw-jms-server
                 "gw-jms-ks" nil
                 "gw-jms-ts" nil
                 "adapters" {"input-adapter"
                              {"in-url" local-gw-jms-server
                               "out-url" local-jms-server
                               "ks" nil
                               "ts" nil
                               "rules" {"a-b" {"in-topic" "/topic/a"
                                               "out-topic" "/topic/b"
                                               "operation" {"name" "no-op"
                                                            "parameters" nil}}}}}}})

(defn jms-broker-fixture [f]
  (let [broker (start-broker local-jms-server)]
    (f)
    (.stop broker)))

(use-fixtures :each jms-broker-fixture)

(deftest test-fixture
  (is true))

(deftest create-single-jms-get-producers-test
  (let [adapter-name (first (keys jms-test-adapter-cfg))
        adapter-cfg (jms-test-adapter-cfg adapter-name)
        adapter (create-single-adapter adapter-cfg)]
    (is (not= nil ((adapter :get-producers) "a-b")))
    (adapter :shutdown)))

(deftest create-single-jms-get-consumers-test
  (let [adapter-name (first (keys jms-test-adapter-cfg))
        adapter-cfg (jms-test-adapter-cfg adapter-name)
        adapter (create-single-adapter adapter-cfg)]
    (is (not= nil ((adapter :get-consumers) "a-b")))
    (adapter :shutdown)))

(deftest simple-adapter-data-forwarding-test
  (let [adapter-name (first (keys jms-test-adapter-cfg))
        adapter-cfg (jms-test-adapter-cfg adapter-name)
        adapter (create-single-adapter adapter-cfg)
        producer (create-producer local-jms-server "/topic/a")
        was-run (prepare-flag)
        received-data (ref "")
        consume-fn (fn [d]
                     (dosync (ref-set received-data d))
                     (set-flag was-run))
        consumer (create-consumer local-jms-server "/topic/b" consume-fn)]
    (is (not (nil? producer)))
    (is (not (nil? consumer)))
    (producer "¡Hola!")
    (await-flag was-run)
    (is (flag-set? was-run))
    (is (= "¡Hola!" @received-data))
    (adapter :shutdown)
    (close producer)
    (close consumer)))

(deftest simple-gw-data-forwarding-test
  (let [gw-name (first (keys jms-test-gw-cfg))
        gw-cfg (jms-test-gw-cfg gw-name)
        gw (create-gw gw-cfg)
        producer (create-producer local-gw-jms-server "/topic/a")
        was-run (prepare-flag)
        received-data (ref "")
        consume-fn (fn [d]
                     (dosync (ref-set received-data d))
                     (set-flag was-run))
        consumer (create-consumer local-jms-server "/topic/b" consume-fn)]
    (is (not (nil? producer)))
    (is (not (nil? consumer)))
    (println "Sending data.")
    (producer "¡Hola!")
    (await-flag was-run)
    (is (flag-set? was-run))
    (is (= "¡Hola!" @received-data))
    (gw :shutdown)
    (close producer)
    (close consumer)))

(deftest simple-gw-p2p-permission-test
  (let [gw-name (first (keys jms-test-gw-cfg))
        gw-cfg (jms-test-gw-cfg gw-name)
        gw (create-gw gw-cfg)]
    (create-producer local-gw-jms-server "/topic/a")
    (is (thrown-with-msg? javax.jms.JMSSecurityException #"User anonymous is not authorized to read from: topic://a" (create-consumer local-gw-jms-server "/topic/a" (fn [_]))))
    (gw :shutdown)))

(deftest simple-gw-sr-master-permission-test
  (let [gw-name (first (keys jms-test-gw-cfg))
        gw-cfg (assoc (jms-test-gw-cfg gw-name) "gw-mode" "sr-master")
        gw (create-gw gw-cfg)]
    (create-producer local-gw-jms-server "/topic/a")
    (create-consumer local-gw-jms-server "/topic/a" (fn [_]))
    (gw :shutdown)))

