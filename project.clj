(defproject event-gateway "0.2.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [clj-assorted-utils "1.9.1"]
                 [fg-netzwerksicherheit/clj-jms-activemq-toolkit "1.99.3"]
                 [crypto-random "1.2.0"]]
  :aot :all
  :main event-gateway.main)
