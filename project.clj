(defproject replikativ-cljs-demo "0.1.0-SNAPSHOT"
  :description "Example project for replikativ in cljs."
  :url "https://github.com/replikativ/replikativ-cljs-demo"

  :min-lein-version "2.5.3"

  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.clojure/clojurescript "1.7.228"]
                 [io.replikativ/replikativ "0.1.3-SNAPSHOT"]
                 [io.replikativ/incognito "0.2.0-beta1"]]

  :plugins [[lein-cljsbuild "1.1.2" :exclusions [[org.clojure/clojure]]]]

  :source-paths ["src"]

  :profiles {:dev {:dependencies [[com.cemerick/piggieback "0.2.1"]]
                   :figwheel {:nrepl-port 7888
                              :nrepl-middleware ["cider.nrepl/cider-middleware"
                                                 "cemerick.piggieback/wrap-cljs-repl"]}
                   :plugins [[lein-figwheel "0.5.0-2"]]}}



  :clean-targets ^{:protect false} ["resources/public/js/compiled" "target"]

  :cljsbuild {:builds
              [{:id "dev"
                :source-paths ["src"]

                :figwheel true

                :compiler {:main replikativ-cljs-demo.core
                           :asset-path "js/compiled/out"
                           :output-to "resources/public/js/compiled/replikativ_cljs_demo.js"
                           :output-dir "resources/public/js/compiled/out"
                           :source-map-timestamp true}}]})
