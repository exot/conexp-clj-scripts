(defproject conexp-clj-programming "nil"
  :min-lein-version "1.3.0"
  :description "Fun with conexp-clj"
  :dependencies [[conexp-clj/conexp-clj "0.0.7-alpha-SNAPSHOT"]]
  :keep-non-project-classes true
  :jvm-opts ["-server", "-Xmx1g"]
  :global-vars {*warn-on-reflection* true}
  ;:test-paths ["src/tests/"]
  )
