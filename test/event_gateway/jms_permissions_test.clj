(ns event-gateway.jms-permissions-test
  (:require [clojure.test :refer :all]
            [clojure.pprint :refer :all])
  (:use [clojure.string :only (join split)]
         clj-assorted-utils.util
         event-gateway.core
         clj-jms-activemq-toolkit.jms)
  (:import (java.util ArrayList)
           (java.util.concurrent ArrayBlockingQueue)
           (javax.jms BytesMessage Connection DeliveryMode Message MessageProducer MessageListener ObjectMessage Session TextMessage Topic)
           (org.apache.activemq ActiveMQConnectionFactory ActiveMQSslConnectionFactory)
           (org.apache.activemq.broker BrokerService)
           (org.apache.activemq.security AuthenticationUser SimpleAuthenticationPlugin)))

(def jms-server-addr "tcp://localhost:31313")
(def test-users [(AuthenticationUser. "test-user" "secret" "test-group")])
(def test-topic "/topic/test.topic.a")

(deftest create-producer-without-permission-test
  (let [broker-service (start-broker jms-server-addr)
        authentication-plugin (doto (SimpleAuthenticationPlugin. test-users)
                                (.setAnonymousAccessAllowed false)
                                (.setAnonymousUser "anonymous")
                                (.setAnonymousGroup "anonymous")
                                (.installPlugin (.getBroker broker-service)))
        producer (create-producer jms-server-addr test-topic)
;        was-run (prepare-flag)
;        consume-fn (fn [_] (set-flag was-run))
;        consumer (create-consumer local-jms-server test-topic consume-fn)
        ]
;    (is (not (nil? producer)))
;    (is (not (nil? consumer)))
;    (producer "¡Hola!")
;    (await-flag was-run)
;    (is (flag-set? was-run))
    (close producer)
;    (close consumer)
    (.stop broker-service)))


