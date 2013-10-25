(ns url-normalizer.utils-test
  (:use
    [url-normalizer.utils]
    [url-normalizer.test]
    [clojure.test])
  (:gen-class))

(deftest test-remove-directory-index
  (let [ctx {:remove-directory-index? true}]
    (is (= (remove-directory-index "/index.php" ctx) "/"))))

(deftest test-decode-reserved-characters
  (let [ctx {:decode-reserved-characters? true}]
    (is (= (normalize-percent-encoding "/%3Ffoo%3Dbar%26bif%3Dbaz" ctx)
           "/?foo=bar&bif=baz"))))

(deftest test-sort-query-keys
  (let [ctx {:sort-query-keys? true}]
    (is (= (sort-query-keys "c=1&c=2&b=3&a=0" ctx) "a=0&b=3&c=1&c=2"))))

(deftest test-remove-query
  (let [ctx {:remove-query? true}]
    (is (= (remove-query "/hello/?" ctx) nil))
    (is (= (remove-query "" ctx) nil))
    (is (= (remove-query "c=1&c=2&b=3&a=0" ctx) nil))))

(deftest test-remove-duplicate-query-keys
  (let [ctx {:remove-duplicate-query-keys? true}]
    (is (= (remove-duplicate-query-keys "" ctx) ""))
    (is (= (remove-duplicate-query-keys "a=0&a=1&a=2&b=0&b=1" ctx) "a=2&b=1"))))

(deftest test-remove-fragment
  (let [ctx {:remove-fragment? true}]
    (is (nil? (remove-fragment "foo" ctx)))
    (is (nil? (remove-fragment "!foo" ctx))))
  (let [ctx {:remove-fragment? true
             :keep-hashbang-fragment? true}]
    (is (nil? (remove-fragment "foo" ctx)))
    (is (= (remove-fragment "!foo" ctx) "!foo"))))

(deftest test-remove-www
  (let [ctx {:remove-www? true}]
    (is (= (remove-www "www.hello.com" ctx) "hello.com"))
    (is (= (remove-www "wwwhello.com" ctx) "wwwhello.com"))
    (is (= (remove-www "www2.hello.com" ctx) "www2.hello.com"))
    (is (= (remove-www "wwww.hello.com" ctx) "wwww.hello.com"))
    (is (= (remove-www "" ctx) ""))
    (is (= (remove-www "www." ctx) ""))))
