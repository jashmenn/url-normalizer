(defproject url-normalizer "0.5.3-1"
  :description "Normalizes and standardizes URLs in a consistent manner."
  :url "https://github.com/retiman/url-normalizer"
  :license {:name "Eclipse Public License - v 1.0"
            :url "http://www.eclipse.org/legal/epl-v10.html"
            :distribution :repo}
  :min-lein-version "2.0.0"
  :warn-on-reflection true
  :repositories
    [["releases" {:url "https://clojars.org/repo" :sign-releases false}]]
  :dependencies
    [[org.clojure/clojure "1.4.0"]
     [org.flatland/ordered "1.4.0"]
     [org.apache.abdera/abdera-i18n "1.1.1" :exclusions
       [commons-codec
        geronimo-activation]]
     [org.apache.httpcomponents/httpclient "4.1" :exclusions
       [commons-logging
        commons-codec]]]
  :dev-dependencies
    [[backtype/autodoc "0.9.0-SNAPSHOT" :exclusions
       [org.clojure/clojure]]
     [robert/hooke "1.1.2" :exclusions
       [org.clojure/clojure]]]
  :test-selectors
    {:default (fn [t] (not (:failing t)))
     :failing :failing
     :all (fn [_] true)})
