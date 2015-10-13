(defproject com.quipucamayoc/sound "0.2.2"
            :description "Quipucamayoc Sound"
            :url "http://quipucamayoc.com/"
            :license {:name "Eclipse Public License"
                      :url  "http://www.eclipse.org/legal/epl-v10.html"}

            :dependencies [[org.clojure/clojure "1.7.0"]
                           [quil "2.2.6" :exclusions [org.clojure/clojure]]
                           [overtone "0.9.1"]
                           [prismatic/schema "1.0.1"]
                           [org.clojure/core.async "0.1.346.0-17112a-alpha"]
                           [com.novemberain/monger "3.0.1"]]

            :jvm-opts ^:replace ["-server"
                                 "-Xmx2g"
                                 "-XX:+AggressiveOpts"
                                 "-XX:+UseG1GC"
                                 "-XX:MaxGCPauseMillis=1"
                                 "-XX:+UseTLAB"]

            :main sound.core

            :source-paths ["src"]
            :resource-paths ["resources"]

            :plugins [[lein-pprint "1.1.2"]
                      [lein-gorilla "0.3.5-SNAPSHOT"]
                      [lein-bikeshed "0.2.0"]
                      [lein-kibit "0.1.2"]
                      [jonase/eastwood "0.2.1"]
                      [lein-ancient "0.6.7"]
                      [lein-marginalia "0.8.0"]])
