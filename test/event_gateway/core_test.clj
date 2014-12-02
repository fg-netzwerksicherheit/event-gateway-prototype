(ns event-gateway.core-test
  (:require [clojure.test :refer :all]
            [clojure.pprint :refer :all]
            [event-gateway.core :refer :all]))

(def no-jms-adapter-cfg
  {"no-jms-test"
    {"in-url" (str no-jms-prefix "://foo:61616")
     "out-url" (str no-jms-prefix "://bar:61616")
     "ks" "nojms-test.ks"
     "ts" "nojms-test.ts"
     "rules" {"a-b" {"in-topic" "a"
                     "out-topic" "b"
                     "operation" {"name" "no-op"
                                  "parameters" nil}}}}})

(def no-jms-gw-cfg
  {"default-gw" {"gw-jms-url" "nojms://foo:51515"
                 "gw-jms-ks" "gw-jms.ks"
                 "gw-jms-ts" "gw-jms.ts"
                 "adapters" no-jms-adapter-cfg}})

(deftest create-single-nojms-adapter-test
  (let [adapter-name (first (keys no-jms-adapter-cfg))
        adapter-cfg (no-jms-adapter-cfg adapter-name)
        adapter (create-single-adapter adapter-cfg)]
    (is (= adapter-cfg (adapter :get-cfg)))))

(deftest single-nojms-adapter-get-rules-test
  (let [adapter-name (first (keys no-jms-adapter-cfg))
        adapter-cfg (no-jms-adapter-cfg adapter-name)
        adapter (create-single-adapter adapter-cfg)]
    (is (= (adapter-cfg "rules" (adapter :get-rules))))))

(deftest single-nojms-adapter-add-rule-test
  (let [adapter-name (first (keys no-jms-adapter-cfg))
        adapter-cfg (no-jms-adapter-cfg adapter-name)
        adapter (create-single-adapter adapter-cfg)
        new-rule {"c-d" {"in-topic" "c" "out-topic" "d"
                         "operation" {"name" "no-op"
                                      "parameters" nil}}}]
    (adapter :add-rule new-rule)
    (is (= (merge (adapter-cfg "rules") new-rule) (adapter :get-rules)))))

(deftest single-nojms-adapter-remove-rule-test
  (let [adapter-name (first (keys no-jms-adapter-cfg))
        adapter-cfg (no-jms-adapter-cfg adapter-name)
        adapter (create-single-adapter adapter-cfg)]
    (adapter :remove-rule "a-b")
    (is (= {} (adapter :get-rules)))))

(deftest create-single-nojms-get-producers-test
  (let [adapter-name (first (keys no-jms-adapter-cfg))
        adapter-cfg (no-jms-adapter-cfg adapter-name)
        adapter (create-single-adapter adapter-cfg)]
    (is (= nil ((adapter :get-producers) "a-b")))))

(deftest create-single-nojms-get-consumers-test
  (let [adapter-name (first (keys no-jms-adapter-cfg))
        adapter-cfg (no-jms-adapter-cfg adapter-name)
        adapter (create-single-adapter adapter-cfg)]
    (is (= nil ((adapter :get-consumers) "a-b")))))

(deftest single-nojms-adapter-remove-rule-producer-consumer-removal-test
  (let [adapter-name (first (keys no-jms-adapter-cfg))
        adapter-cfg (no-jms-adapter-cfg adapter-name)
        adapter (create-single-adapter adapter-cfg)]
    (adapter :remove-rule "a-b")
    (is (= {} (adapter :get-consumers)))
    (is (= {} (adapter :get-producers)))))



(deftest create-single-nojms-gw-test
  (let [gw-name (first (keys no-jms-gw-cfg))
        gw-cfg (no-jms-gw-cfg gw-name)
        gw (create-gw gw-cfg)]
    (is (= gw-cfg (gw :get-cfg)))))

(deftest single-nojms-gw-get-adapters-test
  (let [gw-name (first (keys no-jms-gw-cfg))
        gw-cfg (no-jms-gw-cfg gw-name)
        gw (create-gw gw-cfg)]
    (is (= (gw-cfg "adapters" (gw :get-adapters))))))

(deftest single-nojms-gw-add-adapter-test
  (let [gw-name (first (keys no-jms-gw-cfg))
        gw-cfg (no-jms-gw-cfg gw-name)
        gw (create-gw gw-cfg)
        new-adapter {"no-jms-test-2" {"in-url" (str no-jms-prefix "://foo:61616")
                                      "out-url" (str no-jms-prefix "://bar:61616")
                                      "ks" "nojms-test.ks"
                                      "ts" "nojms-test.ts"
                                      "rules" {"a-b" {"in-topic" "a"
                                                      "out-topic" "b"
                                                      "operation" {"name" "no-op"
                                                                   "parameters" nil}}}}}]
    (gw :add-adapter new-adapter)
    (is (= (merge (gw-cfg "adapters") new-adapter) (gw :get-adapter-configs)))))

(deftest single-nojms-gw-remove-adapter-test
  (let [gw-name (first (keys no-jms-gw-cfg))
        gw-cfg (no-jms-gw-cfg gw-name)
        gw (create-gw gw-cfg)]
    (gw :remove-adapter "no-jms-test")
    (is (= {} (gw :get-adapter-configs)))))

