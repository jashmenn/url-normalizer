(ns url-normalizer.core-test
  (:refer-clojure :exclude (resolve))
  (:use
    [url-normalizer.core]
    [url-normalizer.test]
    [clojure.test])
  (:import
    [java.net URL URI]))

(refer-private 'url-normalizer.core)

(defn- as-uri-map
  [h]
  (apply merge
    (cons {} (map #(vector (as-uri (first %)) (as-uri (second %))) h))))

(def
  ^{:doc
    "Tests from RFC3986: Section 5.3.  The keys should resolve against
    http://a/b/c/d;p?q to the values.

    See <http://www.ietf.org/rfc/rfc3986.txt>"}
  rfc3986-normal-tests
  (as-uri-map
    {"g:h" "g:h"
     "g" "http://a/b/c/g"
     "./g" "http://a/b/c/g"
     "g/" "http://a/b/c/g/"
     "/g" "http://a/g"
     "//g" "http://g"
     "?y" "http://a/b/c/d;p?y"
     "g?y" "http://a/b/c/g?y"
     "#s" "http://a/b/c/d;p?q#s"
     "g#s" "http://a/b/c/g#s"
     "g?y#s" "http://a/b/c/g?y#s"
     ";x" "http://a/b/c/;x"
     "g;x" "http://a/b/c/g;x"
     "g;x?y#s" "http://a/b/c/g;x?y#s"
     "" "http://a/b/c/d;p?q"
     "." "http://a/b/c/"
     "./" "http://a/b/c/"
     ".." "http://a/b/"
     "../" "http://a/b/"
     "../g" "http://a/b/g"
     "../.." "http://a/"
     "../../" "http://a/"
     "../../g" "http://a/g"}))
(def
  ^{:doc
    "Tests from RFC3986 Section 5.4.2.  The keys should resolve against
    http://a/b/c/d;p?q to the values.

    See <http://www.ietf.org/rfc/rfc3986.txt>"}
  rfc3986-abnormal-tests
  (as-uri-map
    {"../../../g" "http://a/g"
     "../../../../g" "http://a/g"
     "/./g" "http://a/g"
     "/../g" "http://a/g"
     "g." "http://a/b/c/g."
     ".g" "http://a/b/c/.g"
     "g.." "http://a/b/c/g.."
     "..g" "http://a/b/c/..g"
     "./../g" "http://a/b/g"
     "./g/." "http://a/b/c/g/"
     "g/./h" "http://a/b/c/g/h"
     "g/../h" "http://a/b/c/h"
     "g;x=1/./y" "http://a/b/c/g;x=1/y"
     "g;x=1/../y" "http://a/b/c/y"
     "g?y/./x" "http://a/b/c/g?y/./x"
     "g?y/../x" "http://a/b/c/g?y/../x"
     "g#s/./x" "http://a/b/c/g#s/./x"
     "g#s/../x" "http://a/b/c/g#s/../x"}))

(def
  ^{:doc
    "Pace tests.  Note that the pace examples remove empty user info as part of
    the normalization process.

    See <http://www.intertwingly.net/wiki/pie/PaceCanonicalIds>"}
  pace-tests
  (as-uri-map
    {"http://:@example.com/" "http://example.com/"
     "http://@example.com/" "http://example.com/"
     "http://example.com" "http://example.com/"
     "HTTP://example.com/" "http://example.com/"
     "http://EXAMPLE.COM/" "http://example.com/"
     "http://example.com/%7Ejane" "http://example.com/~jane"
     "http://example.com/?q=%C3%87" "http://example.com/?q=%C3%87"
     "http://example.com/?q=%E2%85%A0" "http://example.com/?q=%E2%85%A0"
     "http://example.com/?q=%5c" "http://example.com/?q=%5C"
     "http://example.com/?q=%5C" "http://example.com/?q=%5C"
     "http://example.com?q=�" "http://example.com/?q=%C7"
     "http://example.com/?q=Ç" "http://example.com/?q=C%CC%A7"
     "http://example.com/a/../a/b" "http://example.com/a/b"
     "http://example.com/a/./b" "http://example.com/a/b"
     "http://example.com:80/" "http://example.com/"
     "http://example.com/" "http://example.com/"
     "http://example.com/~jane" "http://example.com/~jane"
     "http://example.com/a/b" "http://example.com/a/b"
     "http://example.com:8080/" "http://example.com:8080/"
     "http://user:password@example.com/" "http://user:password@example.com/"}))

(def
  ^{:doc
    "Tests from RFC2396.

    See <http://labs.apache.org/webarch/uri/rev-2002/rfc2396bis.html>"}
  rfc2396bis-tests
  (as-uri-map
    {"http://www.ietf.org/rfc/rfc2396.txt" "http://www.ietf.org/rfc/rfc2396.txt"
     "telnet://192.0.2.16:80/" "telnet://192.0.2.16:80/"
     "ftp://ftp.is.co.za/rfc/rfc1808.txt" "ftp://ftp.is.co.za/rfc/rfc1808.txt"
     "ldap://[2001:db8::7]/c=GB?objectClass?one" "ldap://[2001:db8::7]/c=GB?objectClass?one"
     "mailto:John.Doe@example.com" "mailto:John.Doe@example.com"
     "news:comp.infosystems.www.servers.unix" "news:comp.infosystems.www.servers.unix"
     "tel:+1-816-555-1212" "tel:+1-816-555-1212"
     "urn:oasis:names:specification:docbook:dtd:xml:4.1.2" "urn:oasis:names:specification:docbook:dtd:xml:4.1.2"
     "http://www.w3.org/2000/01/rdf-schema#" "http://www.w3.org/2000/01/rdf-schema#"
     "http://127.0.0.1/" "http://127.0.0.1/"
     "http://127.0.0.1:80/" "http://127.0.0.1/"
     "http://example.com:081/" "http://example.com:81/"
     "http://example.com?q=foo" "http://example.com/?q=foo"
     "http://example.com/?q=foo" "http://example.com/?q=foo"}))

(def
  ^{:doc
    "Tests from RFC1808 and MNot's urlnorm.py.  These tests drop the fragment and
    remove the trailing dot in the host.

    See <http://www.ietf.org/rfc/rfc1808.txt>
    See <http://www.mnot.net/python/urlnorm.py>"}
  rfc1808-tests
  (as-uri-map
    {"/foo/bar/." "/foo/bar/"
     "/foo/bar/./" "/foo/bar/"
     "/foo/bar/.." "/foo/"
     "/foo/bar/../" "/foo/"
     "/foo/bar/../baz" "/foo/baz"
     "/foo/bar/../.." "/"
     "/foo/bar/../../" "/"
     "/foo/bar/../../baz" "/baz"
     "/./foo" "/foo"
     "/foo." "/foo."
     "/.foo" "/.foo"
     "/foo.." "/foo.."
     "/..foo" "/..foo"
     "/./foo/." "/foo/"
     "/foo/./bar" "/foo/bar"
     "/foo/../bar" "/bar"
     "/foo//" "/foo/"
     "/foo///bar//" "/foo/bar/"
     "http://www.foo.com:80/foo" "http://www.foo.com/foo"
     "http://www.foo.com/foo/../foo" "http://www.foo.com/foo"
     "http://www.foo.com:8000/foo" "http://www.foo.com:8000/foo"
     "http://www.foo.com./foo/bar.html" "http://www.foo.com/foo/bar.html"
     "http://www.foo.com.:81/foo" "http://www.foo.com:81/foo"
     "http://www.foo.com/%7ebar" "http://www.foo.com/~bar"
     "http://www.foo.com/%7Ebar" "http://www.foo.com/~bar"
     "ftp://user:pass@ftp.foo.net/foo/bar"
     "ftp://user:pass@ftp.foo.net/foo/bar"
     "http://USER:pass@www.Example.COM/foo/bar"
     "http://USER:pass@www.example.com/foo/bar"
     "http://www.example.com./" "http://www.example.com/"
     "-" "-"
     "http://www.foo.com/?p=529&#038;cpage=1#comment-783" "http://www.foo.com/?p=529&"}))

(def
  ^{:doc
    "These MNot tests give different output from Java's URI class, which follows
    RFC2396.  For example:

      /../foo -> /../foo

    But in the these tests, they normalize to:

      /../foo -> /foo

    Currently, they are failing.  I'll re-enable them once I figure out who is
    right."}
  rfc1808-failing-tests
  (as-uri-map
    {"/foo/bar/../../../baz" "/baz"
     "/foo/bar/../../../../baz" "/baz"
     "/../foo" "/foo"
     "/./../foo" "/foo"}))

(deftest test-reference-resolution
  (let [base (as-uri "http://a/b/c/d;p?q")]
    (doseq [[original resolved] rfc3986-normal-tests]
      (is (equal? (resolve base original) resolved)))
    (doseq [[original resolved] rfc3986-abnormal-tests]
      (is (equal? (resolve base original) resolved))))
  (is (= (normalize "http://www.foo.com/?p=529&#038;cpage=1#comment-783")
         (normalize "/?p=529&#038;cpage=1#comment-783"
                    {:base "http://www.foo.com"}))))

(deftest test-pace
  (doseq [[a b] pace-tests]
    (is (equivalent? a b {:remove-empty-user-info? true}))))

(deftest test-rfc2396bis
  (doseq [[a b] rfc2396bis-tests]
    (is (equivalent? a b))))

(deftest test-rfc1808
  (doseq [[a b] rfc1808-tests]
    (is (equivalent? a b {:remove-fragment? true
                          :remove-trailing-dot-in-host? true}))))

(deftest ^{:failing true} test-rfc1808-failing
  (doseq [[a b] rfc1808-failing-tests]
    (is (equal? a b))
    (is (equivalent? a b {:remove-fragment? true
                          :remove-trailing-dot-in-host? true}))))

(deftest test-normalization
  (is (equal? (normalize "http://www.foo.com/?p=529&#038;cpage=1#comment-783")
              (as-uri "http://www.foo.com/?p=529&#038;cpage=1%23comment-783")))
  (is (equal? (normalize "http://example.com//??##")
              (as-uri "http://example.com/??#%23")))
  (is (equal? (normalize "http://例え.テスト/")
              (as-uri "http://xn--r8jz45g.xn--zckzah/"))))

(deftest
  ^{:doc
    "There is a bug with URI resolution in the Java URI class.  Making use
    of the URI class in conjunction with the Apache HttpComponents library's
    resolve function fixes the problem.  However, the single argument
    constructor to URI cannot handle unencoded URI's.  The URL class can,
    but then we run into the resolution problem.  We can later move towards
    our own URI constructor that avoids this problem.

    See <http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4708535>"
    :failing true}
  test-fixes-java-bug-4708535
  (let [expected (as-uri "http://example.org/dir/file#foo")]
    (is (equal?
          expected
          (resolve (as-uri "http://example.org/dir/file")
                   (as-uri "#foo"))))
    (is (equal?
          expected
          (resolve (as-uri "http://example.org/dir/file#frag")
                   (as-uri "#foo"))))
    (is (equal?
          expected
          (normalize "#foo" {:base "http://example.org/dir/file"})))
    (is (equal?
          expected
          (normalize "#foo" {:base "http://example.org/dir/file#frag"}))))
  (let [expected (as-uri "http://example.org/dir/file")]
    (is (equal?
          expected
          (resolve (as-uri "http://example.org/dir/file")
                   (as-uri ""))))
    (is (equal?
          expected
          (resolve (as-uri "http://example.org/dir/file#frag")
                   (as-uri ""))))
    (is (equal?
          expected
          (normalize "" {:base "http://example.org/dir/file"})))
    (is (equal?
          expected
          (normalize "" {:base "http://example.org/dir/file#frag"})))))

(deftest
  ^{:doc "Tests from RFC3986: 6.2.2.  Syntax-Based Normalization."}
  test-syntax-based-normalization
  (is (equal? (normalize "example://a/b/c/%7Bfoo%7D")
              (normalize "eXAMPLE://a/./b/../b/%63/%7bfoo%7d"))))

(deftest
  ^{:doc "Tests from RFC3986: 6.2.2.1.  Case Normalization."}
  test-case-normalization
  (is (equal? (normalize "HTTP://www.EXAMPLE.com/")
              (normalize "http://www.example.com/")))
  (is (equal? (normalize "http://www.example.com/%7B")
              (normalize "http://www.example.com/%7b"))))

(deftest
  ^{:doc
    "Tests for RFC3986: 6.2.2.2.  Percent-Encoding Normalization.

    Do not percent encode unreserved characters.  When encountered, they
    should be decoded."}
  test-percent-encoding-normalization
  (is (equal? (normalize "http://example.com/~azAZ09-._")
              (normalize "http://example.com/%7E%61%7A%41%5A%30%39%2D%2E%5F"))))

(deftest
  ^{:doc
    "Tests from RFC3986: 6.2.3.  Scheme-Based Normalization.

     The preceding section describes additional normalizations for other schemes.
     For example, the following are equivalent URIs:

     mailto:Joe@example.com
     mailto:Joe@Example.com

     Being a URL normalizer, we will ignore these."}
  test-scheme-based-normalization
  (let [expected (as-uri "http://example.com/")]
    (is (equal? expected (normalize "http://example.com")))
    (is (equal? expected (normalize "http://example.com/")))
    (is (equal? expected (normalize "http://example.com:/")))
    (is (equal? expected (normalize "http://example.com:80/")))
    (is (not (equal? expected (normalize "http://www.example.com/?"))))))

(deftest test-normalize-user-info-part
  (letfn [(f ([uri] (f uri *context*))
             ([uri ctx] (normalize-user-info-part
                          (as-uri uri)
                          (merge *context* ctx))))]
    (is (= (f "http://user@example.com") "user"))
    (is (= (f "http://user:password@example.com") "user:password"))
    (is (= (f "http://@example.com") ""))
    (is (nil? (f "http://example.com")))
    (is (nil? (f "http://@example.com" {:remove-empty-user-info? true})))
    (is (nil? (f "http://:@example.com" {:remove-empty-user-info? true})))))

(deftest test-normalize-host-part
  (letfn [(f ([uri] (f uri *context*))
             ([uri ctx] (normalize-host-part
                          (as-uri uri)
                          (merge *context* ctx))))]
    (is (= (f "http://WWW.EXAMPLE.COM") "www.example.com"))
    (is (= (f "http://www.example.com.") "www.example.com."))
    (is (= (f "http://www.example.com." {:remove-trailing-dot-in-host? true})
           "www.example.com"))
    (is (= (f "http://example.com" {:remove-www? true}) "example.com"))
    (is (= (f "http://www2.example.com" {:remove-www? true})
           "www2.example.com"))
    (is (nil? (f "/")))
    (is (nil? (f "/" {:remove-trailing-dot-in-host? true})))))

(deftest test-normalize-port-part
  (letfn [(f ([uri] (f uri *context*))
             ([uri ctx] (normalize-port-part
                          (as-uri uri)
                          (merge *context* ctx))))]
    (is (= (f "http://example.com:8080") 8080))
    (is (= (f "http://example.com:8080" {:remove-default-port? true}) 8080))
    (is (nil? (f "http://example.com:80" {:remove-default-port? true})))))

(deftest test-normalize-path-part
  (letfn [(f ([uri] (f uri *context*))
             ([uri ctx] (normalize-path-part
                          (as-uri uri)
                          (merge *context* ctx))))]
    (is (= (f "http://example.com") "/"))
    (is (= (f "http://example.com/") "/"))
    (is (= (f "http://example.com/foo/bar/../baz") "/foo/baz"))
    (is (= (f "http://example.com/foo/bar/../baz/") "/foo/baz/"))
    (is (= (f "http://example.com/foo/../..") "/.."))
    (is (= (f "http://example.com/index." {:remove-directory-index? true})
           "/index."))
    (is (= (f "http://example.com/index.html" {:remove-directory-index? true})
           "/"))
    (is (= (f "http://example.com/index.php" {:remove-directory-index? true})
           "/"))))

(deftest test-normalize-fragment-part
  (letfn [(f ([uri] (f uri *context*))
             ([uri ctx] (normalize-fragment-part
                          (as-uri uri)
                          (merge *context* ctx))))]
    (is (= (f "http://example.com##") "#"))
    (is (= (f "http://example.com#foo") "foo"))
    (is (= (f "http://example.com#") ""))
    (is (nil? (f "http://example.com")))
    (is (nil? (f "http://example.com#foo" {:remove-fragment? true})))
    (is (nil? (f "http://example.com/#foo"
              {:remove-fragment? true
               :keep-hashbang-fragment? true})))
    (is (= (f "http://example.com/#!foo"
              {:remove-fragment? true
               :keep-hashbang-fragment? true})
           "!foo"))))

(deftest test-sort-and-remove-duplicate-query-key
  ^{:doc "Fixes issue #8"}
  (is (= (normalize "http://example.net/?a=1&b=2&a=3&A=4"
                    {:remove-duplicate-query-keys? true
                     :sort-query-keys? true})
         (as-uri "http://example.net/?A=4&a=3&b=2"))))

(deftest test-removes-default-port-with-mixed-case-scheme
  ^{:doc "Fixes issue #9"}
  (is (= (normalize "http://example.com:80/")
         (as-uri "http://example.com/")))
  (is (= (normalize "hTtP://example.com:80/")
         (as-uri "http://example.com/")))
  (is (= (normalize "https://example.com:443/")
         (as-uri "https://example.com/")))
  (is (= (normalize "hTtPs://example.com:443/"
                    {:force-http? true})
         (as-uri "http://example.com/")))
  (is (= (normalize "https://example.com:443/"
                    {:force-http? true})
         (as-uri "http://example.com/"))))
