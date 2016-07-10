(defproject clj-url-shortener "1.0-SNAPSHOT"
            :main clj-url-shortener.core
            :profiles {:uberjar {:aot :all}}
            :dependencies [[org.clojure/clojure "1.7.0"]
                           [ring/ring-core "1.4.0"]
                           [ring/ring-jetty-adapter "1.4.0"]
                           [ring/ring-defaults "0.2.0"]
                           [compojure "1.5.0"]
                           [commons-validator/commons-validator "1.4.0"]
                           ]
            :plugins [[lein-ring "0.9.7"]]
            :ring {:handler clj-url-shortener.core/app})
