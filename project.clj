(defproject quipucamayoc "0.1.5"
            :description "Quipucamayoc Core"
            :url "http://example.com/FIXME"
            :license {:name "Eclipse Public License"
                      :url  "http://www.eclipse.org/legal/epl-v10.html"}
            :dependencies [[org.clojure/clojure "1.7.0-alpha3"]
                           [quil "2.2.2"]
                           [overtone "0.9.1"]
                           [org.clojure/core.async "0.1.346.0-17112a-alpha"]
                           ;[eu.cassiel/clojure-zeroconf "1.2.0"]
                           [org.clojure/math.numeric-tower "0.0.4"]
                           ;[com.cognitect/transit-clj "0.8.259"]
                           ]
            :main ^:skip-aot quipucamayoc.core
            ;:aot [quipucamayoc.core]
            :plugins [[lein-pprint "1.1.1"]
                      [lein-gorilla "0.3.3"]
                      [lein-ancient "0.6.0-SNAPSHOT"]])