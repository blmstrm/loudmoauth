(defproject loudmoauth "0.1.2"
  :description "A single user multi provider oauth2 client library."
  :url "http://github.com/blmstrm/loudmoauth"
  :license {:name "The MIT License (MIT)"
            :url "http://opensource.org/licenses/MIT"}
  :plugins [[lein-cloverage "1.0.6"]]
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.clojure/data.codec "0.1.0"]
                 [org.clojure/data.json "0.2.6"]
                 [clj-http "3.1.0"]
                 [prismatic/schema "1.1.2"]])
