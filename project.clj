(defproject quipucamayoc "0.2.2"
            :description "Quipucamayoc Core *&* Sound"
            :url "http://quipucamayoc.com/"
            :license {:name "Eclipse Public License"
                      :url  "http://www.eclipse.org/legal/epl-v10.html"}

            :dependencies [[org.clojure/clojure "1.7.0-alpha5"]
                           [quil "2.2.5" :exclusions [org.clojure/clojure]]
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
                      [lein-gorilla "0.3.5-SNAPSHOT"]
                      [lein-bikeshed "0.2.0"]
                      [lein-kibit "0.0.8"]
                      [lein-hiera "0.9.0"]
                      [jonase/eastwood "0.2.1"]
                      [lein-ancient "0.6.5"]
                      [lein-marginalia "0.8.0"]]

            :hera {:path "target/ns-hierarchy.png"
              :vertical true
               :show-external true
                :cluster-depth 0
                 :trim-ns-prefix true
                  :ignore-ns #{}})
