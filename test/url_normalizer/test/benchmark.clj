(ns url-normalizer.test.benchmark
  (:refer-clojure :exclude (resolve))
  (:use
    [url-normalizer.core])
  (:gen-class))

(defn -main [& args]
  (println "Previous version:")
  (println "Elapsed time: 2.812006 msecs")
  (println "Elapsed time: 0.38123 msecs")
  (println "Elapsed time: 0.959255 msecs")
  (println "Elapsed time: 0.49435 msecs")
  (println)
  (println "This version:")
  (time (println (normalize "ldap://[2001:db8::7]/c=GB?objectClass?one")))
  (time (println (normalize "/./foo/.")))
  (time (println (normalize "http://www.foo.com/?p=529&#038;cpage=1#comment-783")))
  (time (println (normalize "http://:@example.com/"))))
