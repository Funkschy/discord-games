(defproject discord-games "0.1.0-SNAPSHOT"
  :description "Play games directly inside of discord"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[org.clojure/clojure "1.11.1"]
                 [org.clojure/tools.logging "1.2.4"]
                 [org.clojure/core.async "1.5.648"]
                 [org.suskalo/discljord "1.3.0"
                  :exclusions [org.clojure/clojure
                               org.clojure/tools.logging
                               org.clojure/core.async]]]
  :main ^:skip-aot discord-games.core
  :global-vars {*warn-on-reflection* true}
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all
                       :jvm-opts ["-Dclojure.compiler.direct-linking=true"]}})
