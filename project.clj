(defproject quipucamayoc "0.1.9"
            :description "Quipucamayoc Core"
            :url "http://quipucamayoc.com/"
            :license {:name "Eclipse Public License"
                      :url  "http://www.eclipse.org/legal/epl-v10.html"}

            :dependencies [[org.clojure/clojure "1.7.0-alpha4"]
                           [quil "2.2.2"]
                           [overtone "0.9.1"]
                           [org.clojure/core.async "0.1.346.0-17112a-alpha"]
                           [com.cognitect/transit-clj "0.8.259"]]

            :jvm-opts ^:replace ["-server"
                                 "-Xmx512m"
                                 "-XX:+UseG1GC"
                                 "-XX:MaxGCPauseMillis=1"
                                 "-XX:+UseTLAB"]

            :main ^:skip-aot quipucamayoc.core

            :plugins [[lein-pprint "1.1.2"]
                      [lein-gorilla "0.3.3"]
                      [lein-ancient "0.6.0-SNAPSHOT"]])