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
           (org.apache.activemq.security AuthenticationUser AuthorizationEntry AuthorizationMap AuthorizationPlugin DefaultAuthorizationMap SimpleAuthenticationPlugin)))

(def jms-server-addr "tcp://localhost:31313")
(def test-user-name "test-user")
(def test-user-password "secret")
(def test-user-group "test-group")
(def test-users [(AuthenticationUser. test-user-name test-user-password test-user-group)])
(def test-topic "/topic/test.topic.a")

(deftest create-producer-without-permission-test
  (let [
        ;broker-service (start-broker jms-server-addr)
        authentication-plugin (doto (SimpleAuthenticationPlugin. test-users)
                                (.setAnonymousAccessAllowed false)
                                (.setAnonymousUser "anonymous")
                                (.setAnonymousGroup "anonymous"))
        default-authorization-entry (doto (AuthorizationEntry.)
                                      (.setAdmin test-user-name)
                                      (.setRead test-user-name)
                                      (.setWrite test-user-name))
        default-authorization-map (doto (DefaultAuthorizationMap.)
                                    (.setDefaultEntry default-authorization-entry))
        authorization-plugin (AuthorizationPlugin. default-authorization-map)
        broker-service (doto (BrokerService.)
                         (.addConnector jms-server-addr)
                         (.setPersistent false)
                         (.setUseJmx false)
                         (.setPlugins (into-array org.apache.activemq.broker.BrokerPlugin [authorization-plugin authorization-plugin]))
                         (.start))
;        was-run (prepare-flag)
;        consume-fn (fn [_] (set-flag was-run))
;        consumer (create-consumer local-jms-server test-topic consume-fn)
        ]
;    (is (not (nil? producer)))
;    (is (not (nil? consumer)))
;    (producer "¡Hola!")
;    (await-flag was-run)
;    (is (flag-set? was-run))
    (is (thrown-with-msg? javax.jms.JMSSecurityException #"User is not authenticated." (create-producer jms-server-addr test-topic)))
;    (close consumer)
    (.stop broker-service)))


