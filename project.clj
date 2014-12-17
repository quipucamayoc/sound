(defproject quipucamayoc "0.2.1"
            :description "Quipucamayoc Core *&* Sound"
            :url "http://quipucamayoc.com/"
            :license {:name "Eclipse Public License"
                      :url  "http://www.eclipse.org/legal/epl-v10.html"}

            :dependencies [[org.clojure/clojure "1.7.0-alpha4"]
                           [quil "2.2.4"]
                           [overtone "0.9.1"]
                           [org.clojure/core.async "0.1.346.0-17112a-alpha"]]

            :jvm-opts ^:replace ["-server"
                                 "-Xmx512m"
                                 "-XX:+UseG1GC"
                                 "-XX:MaxGCPauseMillis=1"
                                 "-XX:+UseTLAB"]

            :main ^:skip-aot quipucamayoc.core

            :source-paths ["src"]
            :resource-paths ["resources"]

            :plugins [[lein-pprint "1.1.2"]
                      [lein-gorilla "0.3.3"]
                      [lein-ancient "0.6.0-SNAPSHOT"]
                      [lein-marginalia "0.8.0"]])