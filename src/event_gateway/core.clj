(ns event-gateway.core
  (:use clj-jms-activemq-toolkit.jms
        crypto.random))

(def no-jms-prefix "nojms")

(def ex-msg-rule-exists "Rule exists!")
(def ex-msg-adapter-exists "Adapter exists!")

(defn create-single-adapter
  ([cfg]
   (create-single-adapter cfg {}))
  ([cfg common-gw-state]
   (println "Creating adapter:" cfg)
   (let [rules (ref {})
         in-url (cfg "in-url")
         out-url (cfg "out-url")
         ks (cfg "ks")
         ts (cfg "ts")
         producers (ref {})
         consumers (ref {})
         remove-rule-fn (fn [rule-name]
                          (let [con (consumers rule-name)
                                prod (producers rule-name)]
                            (if (not (nil? con)) (close con))
                            (dosync (alter consumers dissoc rule-name))
                            (if (not (nil? prod)) (close prod))
                            (dosync (alter producers dissoc rule-name))
                            (dosync (alter rules dissoc rule-name))))
         self-fn (fn [cmd & args]
                   (condp = cmd
                     :add-rule (let [_ (println "Adding rule:" args)
                                     rule-name (-> args first keys first)
                                     _ (if (contains? @rules rule-name)
                                         (throw (RuntimeException. ex-msg-rule-exists)))
                                     rule ((first args) rule-name)
                                     prod (when (not (.startsWith out-url no-jms-prefix))
                                            (println "Creating GW producer:" out-url (rule "out-topic"))
                                            (create-producer out-url (rule "out-topic")))
                                     forward-fn (fn [data]
                                                  (prod data))
                                     consu (when (not (.startsWith in-url no-jms-prefix))
                                             (println "Creating GW consumer:" in-url (rule "in-topic"))
                                             (if (= in-url (common-gw-state "gw-jms-url"))
                                               (binding [*user-name* (common-gw-state "user-name") *user-password* (common-gw-state "user-password")]
                                                 (create-consumer in-url (rule "in-topic") forward-fn))
                                               (create-consumer in-url (rule "in-topic") forward-fn)))]
                                 (dosync (alter rules assoc rule-name rule))
                                 (dosync (alter producers assoc rule-name prod))
                                 (dosync (alter consumers assoc rule-name consu)))
                     :get-config (assoc cfg "rules" @rules)
                     :get-consumers @consumers
                     :get-producers @producers
                     :get-rules @rules
                     :remove-rule (remove-rule-fn (first args))
                     :shutdown (doseq [rule-name (keys @rules)]
                                 (remove-rule-fn rule-name))
                     nil))]

    (doseq [rule (cfg "rules")]
      (self-fn :add-rule {(key rule) (val rule)}))
    self-fn)))

(defn create-gw
  [cfg]
  (let [adapter-configs (ref {})
        gw-jms-url (cfg "gw-jms-url")
        gw-mode (if (nil? (cfg "gw-mode"))
                  "p2p"
                  (cfg "gw-mode"))
        common-gw-state (if (= gw-mode "p2p")
                          {"gw-jms-url" gw-jms-url, "user-name" "gw-user", "user-password" (base64 16)}
                          {})
        ks (cfg "gw-jms-ks")
        ts (cfg "gw-jms-ts")
        adapters (ref {})
        gw-jms-broker (ref (when (not (.startsWith gw-jms-url no-jms-prefix))
                             (println "Starting GW JMS broker at:" gw-jms-url)
                             (condp = gw-mode
                               "p2p" (start-broker
                                       gw-jms-url true [{"name" (common-gw-state "user-name") "password" (common-gw-state "user-password") "groups" "gw-group"}]
                                       [{"target" ">", "type" "topic", "admin" "gw-group", "read" "gw-group", "write" "gw-group"}
                                        {"target" ">", "type" "topic", "write" "anonymous"}
                                        {"target" "ActiveMQ.Advisory.Producer.Topic.*", "type" "topic", "admin" "anonymous", "write" "anonymous"}
                                        {"target" "ActiveMQ.Advisory.TempQueue,ActiveMQ.Advisory.TempTopic", "type" "topic", "read" "anonymous"}])
                               "sr-master" (start-broker gw-jms-url)
                               nil)))
        remove-adapter-fn (fn [adapter-name]
                            (let [adapt (@adapters adapter-name)]
                              (if (not (nil? adapt))
                                (adapt :shutdown)))
                            (dosync (alter adapters dissoc adapter-name))
                            (dosync (alter adapter-configs dissoc adapter-name)))
        get-adapter-configs-fn #(reduce
                                  (fn [m k]
                                    (assoc-in m [k "rules"] ((@adapters k) :get-rules)))
                                  @adapter-configs (keys @adapters))
        self-fn (fn [cmd & args]
                  (condp = cmd
                    :add-adapter (let [adapter-name (-> args first keys first)
                                       _ (if (contains? @adapter-configs adapter-name)
                                           (throw (RuntimeException. ex-msg-adapter-exists)))
                                       adapter-cfg ((first args) adapter-name)
                                       adapter (create-single-adapter adapter-cfg common-gw-state)]
                                   (dosync (alter adapters assoc adapter-name adapter))
                                   (dosync (alter adapter-configs assoc adapter-name (dissoc adapter-cfg "rules"))))
                    :add-adapter-rule ((@adapters (first args)) :add-rule (second args))
                    :get-config (assoc cfg "adapters" (get-adapter-configs-fn))
                    :get-adapters @adapters
                    :get-adapter-configs  (get-adapter-configs-fn)
                    :remove-adapter (remove-adapter-fn (first args))
                    :remove-adapter-rule ((@adapters (first args)) :remove-rule (second args))
                    :shutdown (do
                                (doseq [adapter-name (keys @adapter-configs)]
                                  (remove-adapter-fn adapter-name))
                                (if (not (nil? @gw-jms-broker))
                                  (.stop @gw-jms-broker)))
                    nil))]
    (doseq [adapter-cfg (cfg "adapters")]
      (println "Adding adapter:" (key adapter-cfg))
      (self-fn :add-adapter {(key adapter-cfg) (val adapter-cfg) }))
    self-fn))


