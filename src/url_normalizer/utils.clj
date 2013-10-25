(ns url-normalizer.utils
  "Utilities and specific normalizations."
  (:require
    [clojure.string :as s])
  (:use
    [flatland.ordered.map])
  (:import
    [java.net InetAddress URI URLEncoder]))

(defn- byte-to-hex-string
  "Converts the lower 16 bits of b to into a hex string."
  [b]
  (let [s (Integer/toHexString (bit-and 0xff b))]
    (if (= (count s) 1) (str "0" s) s)))

(def
  ^{:doc "The default ports for various schemes."}
  default-port
  {"ftp" 21
   "telnet" 23
   "http" 80
   "gopher" 70
   "news" 119
   "nntp" 119
   "prospero" 191
   "https" 443
   "snews" 563
   "snntp" 563})

(def
  ^{:doc "Maps percent encoded octets to alpha characters."}
  alpha
  (let [xs (concat (range 0x41 (inc 0x5A)) (range 0x61 (inc 0x7A)))]
    (zipmap (map #(str "%" (s/upper-case (byte-to-hex-string %))) xs)
            (map #(str (char %)) xs))))

(def
  ^{:doc "Maps percent encoded octets to digits."}
  digits
  (let [xs (range 0x30 (inc 0x39))]
    (zipmap (map #(str "%" (byte-to-hex-string %)) xs)
            (map #(str (char %)) xs))))

(def
  ^{:doc
    "A mapping of encoded unreserved characters to their decoded
    counterparts."}
  unreserved
  (assoc
    (merge alpha digits)
    "%2D" "-"
    "%2E" "."
    "%5F" "_"
    "%7E" "~"))

(def
  ^{:doc
    "A mapping of encoded reserved characters to their decoded
    counterparts."}
  reserved
  (let [cs (map str ":/?#[]@!$&'()*+,;=")]
    (zipmap (map #(URLEncoder/encode %) cs) cs)))

(def
  ^{:doc "A list of functions that decode alphanumerics in a String."}
  decode-alphanum
  (concat
    (map #(fn [#^String s] (.replaceAll s (first %) (last %)))
         (concat alpha digits))))

(defn get-user-info [#^URI uri ctx]
  (.getUserInfo uri))

(defn get-path [#^URI uri ctx]
  (if (:remove-dot-segments? ctx)
    (-> uri (.normalize) (.getPath))
    (.getPath uri)))

(defn get-query [#^URI uri ctx]
  (.getQuery uri))

(defn get-fragment [#^URI uri ctx]
  (.getFragment uri))

(defn lower-case-scheme
  "A safe normalization that lower cases the scheme:

  HTTP://example.com -> http://example.com"
  [scheme ctx]
  (if (:lower-case-scheme? ctx)
    (s/lower-case scheme)
    scheme))

(defn force-http
  "An unsafe normalization that forces the HTTP scheme if HTTPS is encountered:

  https://example.com -> http://example.com"
  [scheme ctx]
  (if (and (:force-http? ctx) (= scheme "https"))
    "http"
    scheme))

(defn remove-www
  "An unsafe normalization that removes the www from a domain:

  http://www.example.com/ -> http://example.com/
  http://www.foo.bar.example.com/ -> http://www.foo.bar.example.com/
  http://www2.example.com/ -> http://www2.example.com/"
  [^String host ctx]
  (if (and (:remove-www? ctx) (.startsWith host "www."))
    (.substring host 4 (count host))
    host))

(defn lower-case-host
  "A safe normalization that lower cases the host name:

  http://ExAmpLe.com -> http://example.com"
  [host ctx]
  (if (:lower-case-host? ctx)
    (s/lower-case host)
    host))

(defn remove-ip
  "An unsafe normalization that removes the IP:

  http://192.0.32.10 -> http://example.com"
  [host ctx]
  (if (:remove-ip? ctx)
    (.getHostName (InetAddress/getByName host))
    host))

(defn remove-empty-user-info
  "An unsafe normalization that removes the user info part of a URI:

  http://@example.com -> http://example.com
  http://:@example.com -> http://example.com"
  [user-info ctx]
  (if-not (and (:remove-empty-user-info? ctx)
               (or (= ":" user-info) (= "" user-info)))
    user-info))

(defn remove-trailing-dot-in-host
  "An unsafe normalization that removes the trailing dot in a host:

  http://example.com./foo -> http://example.com/foo"
  [host ctx]
  (if (and (:remove-trailing-dot-in-host? ctx)
           (= \. (last host)))
    (apply str (butlast host))
    host))

(defn remove-default-port
  "A safe normalization that removes the the default port:

  http://example.com:80 -> http://example.com
  http://example.com:8080 -> http://example.com/"
  [scheme port ctx]
  (if-not (and (:remove-default-port? ctx)
               (= port (get default-port (s/lower-case scheme))))
    port))

(defn normalize-percent-encoding
  "Applies several percent encoding normalizations.

  upper-case-percent-encoding:
  http://example.com/%7ejane -> http://example.com/%7Ejane

  decode-unreserved-characters:
  http://example.com/%7ejane -> http://example.com~/jane

  decode-reserved-characters:
  http://example.com//%3Ffoo%3Dbar%26bif%3Dbaz -> http://example.com/?foo=bar&bif=baz"
  [#^String text ctx]
  (loop [sb (StringBuilder.)
         m (re-matcher #"%[a-fA-F0-9]{2}" text)
         k 0]
    (if (nil? (re-find m))
      (do
        (.append sb (.substring text k))
        (.toString sb))
      (let [g (.group m 0)
            t (.toUpperCase g)]
        (.append sb (.substring text k (.start m)))
        (cond
          (and (:decode-unreserved-characters? ctx)
               (contains? unreserved t))
            (.append sb (get unreserved t))
          (and (:decode-reserved-characters? ctx)
               (contains? reserved t))
            (.append sb (get reserved t))
          :default
            (if (:upper-case-percent-encoding? ctx)
              (.append sb t)
              (.append sb g)))
        (recur sb m (.end m))))))

(defn remove-directory-index
  "An unsafe normalization that removes the directory index from the path.

  http://www.example.com/index.php -> http://www.example.com/"
  [path ctx]
  (if (and (:remove-directory-index? ctx)
           (re-matches #"/index\.[^/]+" path))
    "/"
    path))

(defn add-trailing-slash
  "A safe normalization that adds a slash to an empty path:

  http://example.com -> http://example.com/
  http://example.com/foo -> http://example.com/foo"
  [path ctx]
  (if (and (:add-trailing-slash? ctx) (= "" path)) "/" path))

(defn remove-duplicate-query-keys
  "An unsafe normalization that removes duplicate query keys and values:

  http://example.com/?foo&foo=bar -> http://example.com/?foo=bar"
  [query ctx]
  (if (and (:remove-duplicate-query-keys? ctx)
           (not (s/blank? query)))
    (let [entries (map #(s/split % #"=") (s/split query #"&"))
          query-map (into (ordered-map) entries)]
      (s/join "&" (map #(s/join "=" %) query-map)))
    query))

(defn sort-query-keys
  "An unsafe normalization that sorts the query keys and values:

  http://example.com/?c&a&b -> http://example.com/a&b&c"
  [query ctx]
  (if (:sort-query-keys? ctx)
    ((comp #(s/join "&" %)
           sort
           #(s/split % #"&"))
       query)
    query))

(defn remove-empty-query
  "An unsafe normalization that removes an empty query:

  http://example.com/? -> http://example.com/
  http://example.com? -> http://example.com"
  [query ctx]
  (if-not (and (:remove-empty-query? ctx) (= query ""))
    query))

(defn remove-query
  "An unsafe normalization that removes entire query:

  http://example.com/?foo=bar -> http://example.com/
  http://example.com? -> http://example.com"
  [query ctx]
  (if-not (:remove-query? ctx)
    query))

(defn remove-fragment
  "An unsafe normalization that removes the fragment.  The URI will still refer
  to the same resource so sometimes the fragment is not needed:

  remove-fragment:
  http://example.com/#foo -> http://example.com/

  remove-fragment and keep-hashbang-fragment:
  http://twitter.com/#foo -> http://twitter.com/#foo
  http://twitter.com/#!/user -> http://twitter.com/#!/user

  See <http://code.google.com/web/ajaxcrawling/docs/getting-started.html>
  See <http://www.tbray.org/ongoing/When/201x/2011/02/09/Hash-Blecch>"
  [^String fragment ctx]
  (let [keep? (and (:keep-hashbang-fragment? ctx) (.startsWith fragment "!"))]
    (if-not (and (:remove-fragment? ctx) (not keep?))
      fragment)))
