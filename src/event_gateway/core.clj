(ns event-gateway.core
  (:use clj-jms-activemq-toolkit.jms))

(def no-jms-prefix "nojms")

(def ex-msg-rule-exists "Rule exists!")
(def ex-msg-adapter-exists "Adapter exists!")

(defn create-single-adapter
  [cfg]
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
                                            (create-consumer in-url (rule "in-topic") forward-fn))]
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
    self-fn))

(defn create-gw
  [cfg]
  (let [adapter-configs (ref {})
        gw-jms-url (cfg "gw-jms-url")
        ks (cfg "gw-jms-ks")
        ts (cfg "gw-jms-ts")
        adapters (ref {})
        gw-jms-broker (ref (when (not (.startsWith gw-jms-url no-jms-prefix))
                             (println "Starting GW JMS broker at:" gw-jms-url)
                             (start-broker gw-jms-url)))
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
                                       adapter (create-single-adapter adapter-cfg)]
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
      (self-fn :add-adapter {(key adapter-cfg) (val adapter-cfg)}))
    self-fn))


