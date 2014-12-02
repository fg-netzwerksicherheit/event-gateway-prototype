;;;
;;;   Copyright 2014, Frankfurt University of Applied Sciences
;;;
;;;   This software is released under the terms of the Eclipse Public License 
;;;   (EPL) 1.0. You can find a copy of the EPL at: 
;;;   http://opensource.org/licenses/eclipse-1.0.php
;;;

(ns
  ^{:author "Ruediger Gad",
    :doc "Main class to start an event gateway instance."}
  event-gateway.main
  (:use clojure.pprint
        clojure.tools.cli
        clj-jms-activemq-toolkit.jms
        event-gateway.core)
  (:gen-class))

(defn -main [& args]
  (let [cli-args (cli args
                      ["-c" "--config-file" 
                       "Path to the gateway startup config file." 
                       :default "default-gw-config.cfg"]
                      ["-d" "--daemon" "Run as daemon." :flag true :default false]
                      ["-h" "--help" "Print this help." :flag true])
        arg-map (cli-args 0)
        extra-args (cli-args 1)
        help-string (cli-args 2)]
    (if (arg-map :help)
      (println help-string)
      (do
        (println "Starting event gateway with the following parameters:")
        (pprint arg-map)
        (pprint extra-args)
        (let [read-cfg (read-string (slurp (arg-map :config-file)))
              _ (print "Read config: ")
              _ (pprint read-cfg)
              gw-name (-> read-cfg keys first)
              gw-startup-cfg (read-cfg gw-name)
              _ (println "Starting gateway:" gw-name "with config:")
              _ (pprint gw-startup-cfg)
              gw (create-gw gw-startup-cfg)
;              broker-service (start-broker url)
;              broker-info-producer (create-producer url "/topic/broker.info")
;              broker-info-fn (fn [msg]
;                               (condp (fn [t m] (= (type m) t)) msg
;                                 java.lang.String (condp (fn [v m] (.startsWith m v)) msg
;                                                    "reply" nil ; We ignore all replies for now.
;                                                    "command get-destinations"
;                                                      (let [dst-vector (get-destinations broker-service)
;                                                            dst-string (str "reply destinations " (clojure.string/join " " dst-vector))]
;                                                        (broker-info-producer dst-string))
;                                                    "command get-destinations-with-producers"
;                                                      (let [dst-vector (get-destinations-with-producers broker-service)
;                                                            dst-string (str "reply destinations " (clojure.string/join " " dst-vector))]
;                                                        (broker-info-producer dst-string))
;                                                    (send-error-msg broker-info-producer (str "Unknown broker.info command: " msg)))
;                                 (send-error-msg broker-info-producer (str "Invalid data type for broker.info message: " (type msg)))))
;              broker-info-consumer (create-consumer url "/topic/broker.info" broker-info-fn)
              shutdown-fn (fn []
;                            (broker-info-producer :close)
;                            (broker-info-consumer :close)
                            (gw :shutdown)
                            )]
          ;;; Running the main from, e.g., leiningen results in stdout not being properly accessible.
          ;;; Hence, this will not work when run this way but works when run from a jar via "java -jar ...".
          (if (:daemon arg-map)
            (-> (agent 0) (await))
            (do
              (println "Gateway started... Type \"q\" followed by <Return> to quit: ")
              (while (not= "q" (read-line))
                (println "Type \"q\" followed by <Return> to quit: "))
              (println "Shutting down...")
              (shutdown-fn))))))))

