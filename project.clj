(defproject event-gateway "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [clj-assorted-utils "1.7.0"]
                 [clj-jms-activemq-toolkit "1.1.1"]
                 [crypto-random "1.2.0"]]
  :aot :all
  :main event-gateway.main)
