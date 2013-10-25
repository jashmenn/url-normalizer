(ns url-normalizer.core
  "A Clojure library for normalizing urls with configurable aggressiveness."
  (:refer-clojure :exclude (resolve))
  (:use
    [url-normalizer.utils])
  (:require
    [clojure.java.io :as io])
  (:import
    [java.net URL URI URISyntaxException MalformedURLException]
    [org.apache.abdera.i18n.iri IDNA]
    [org.apache.http.client.utils URIUtils]))

(defn as-url
  ([arg]
    (URL. arg))
  ([ctx arg]
    (URL. (URL. ctx) arg)))

(defmulti as-uri class)
(defmethod as-uri URI [arg] arg)
(defmethod as-uri URL [#^URL arg]
  (URI. (.getProtocol arg)
        (.getUserInfo arg)
        (.getHost arg)
        (.getPort arg)
        (.getPath arg)
        (.getQuery arg)
        (.getRef arg)))
; TODO: This is quite an inelegant solution to problem URIs like:
; http://www.foo.com/?p=529&#038;cpage=1#comment-783 but it'll do for now.
(defmethod as-uri String [arg]
  (try
    (URI. arg)
    (catch URISyntaxException e (as-uri (URL. arg)))))

(def safe-normalizations
  ^{:doc "These are safe normalizations that will not change the semantics of a
         URI. See the #'url-normalizer.utils namespace for additional details
         and implementations."
    :private true}
  {:lower-case-scheme? true
   :lower-case-host? true
   :upper-case-percent-encoding? true
   :decode-unreserved-characters? true
   :add-trailing-slash? true
   :remove-default-port? true
   :remove-dot-segments? true})

(def unsafe-normalizations
  ^{:doc "These are unsafe normalizations that can either change the semantics
         of the URI or cause it to refer to a different resource.  See the
         #'url-normalizer.utils namespace for additional details and
         implementations."
    :private true}
  {:remove-directory-index? false
   :remove-fragment? false
   :remove-ip? false
   :remove-duplicate-query-keys? false
   :remove-empty-query? false
   :remove-query? false
   :remove-empty-user-info? false
   :remove-trailing-dot-in-host? false
   :keep-hashbang-fragment? false
   :force-http? false
   :remove-www? false
   :sort-query-keys? false
   :decode-reserved-characters? false})

(def
  ^{:doc
    "A normalization context. See #'url-normalizer/*safe-normalizations* and
     #'url-normalizer/*unsafe-normalizations* for possible normalizations."
    :dynamic true}
  *context*
  (merge safe-normalizations unsafe-normalizations))

(defn- normalize-scheme-part [#^URI uri ctx]
  (if-let [scheme (.getScheme uri)]
    ((comp #(force-http % ctx)
           #(lower-case-scheme % ctx))
       scheme)))

(defn- normalize-user-info-part [#^URI uri ctx]
  (if-let [user-info (get-user-info uri ctx)]
    (remove-empty-user-info user-info ctx)))

(defn- normalize-host-part [#^URI uri ctx]
  (let [host (.getHost uri)
        authority (.getAuthority uri)]
    (cond
      (not (nil? host))
        ((comp #(remove-www % ctx)
               #(remove-trailing-dot-in-host % ctx)
               #(remove-ip % ctx)
               #(lower-case-host % ctx))
           host)
      (and (not (nil? authority))
           (or (-> uri (.getScheme) (.equalsIgnoreCase "http"))
               (-> uri (.getScheme) (.equalsIgnoreCase "https"))))
        (IDNA/toASCII authority)
      :default
        nil)))

(defn normalize-port-part [#^URI uri ctx]
  (let [scheme (.getScheme uri)
        port (.getPort uri)]
    (if (and port (not= -1 port))
      (remove-default-port scheme port ctx))))

(defn normalize-path-part [#^URI uri ctx]
  (if-let [path (get-path uri ctx)]
    ((comp #(add-trailing-slash % ctx)
           #(remove-directory-index % ctx)
           #(normalize-percent-encoding % ctx))
       path)))

(defn- normalize-query-part [#^URI uri ctx]
  (if-let [query (get-query uri ctx)]
    ((comp #(remove-query % ctx)
           #(remove-empty-query % ctx)
           #(remove-duplicate-query-keys % ctx)
           #(sort-query-keys % ctx)
           #(normalize-percent-encoding % ctx))
       query)))

(defn- normalize-fragment-part [#^URI uri ctx]
  (if-let [fragment (get-fragment uri ctx)]
    ((comp #(remove-fragment % ctx)
           #(normalize-percent-encoding % ctx))
       fragment)))

(defn resolve
  "Resolve a URI reference against a base URI by removing dot segments.  The
  Apache HttpClient version is used instead of the resolve method on URI due
  to a bug in the Java standard library.

  See <http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4708535>"
  [base uri]
  (URIUtils/resolve #^URI (as-uri base)
                    #^URI (as-uri uri)))

(defn normalize
  "By default normalizes a URI using safe normalizations.  The URI is expected
  to be a URL with either the HTTP or HTTPS scheme.

  You may specify a normalization context in order to apply non-semantic
  preserving normalizations."
  ([arg]
    (normalize arg *context*))
  ([arg context]
    (let [ctx (merge *context* context)
          uri (if (:base ctx)
                (as-uri (as-url (:base ctx) arg))
                (as-uri arg))
          authority (.getRawAuthority #^URI uri)
          scheme (normalize-scheme-part uri ctx)
          user-info (normalize-user-info-part uri ctx)
          host (normalize-host-part uri ctx)
          port (normalize-port-part uri ctx)
          path (normalize-path-part uri ctx)
          query (normalize-query-part uri ctx)
          fragment (normalize-fragment-part uri ctx)]
      (if (and (nil? authority) (.isAbsolute #^URI uri))
        (URI. scheme (.getSchemeSpecificPart #^URI uri) fragment)
        (URI. scheme user-info host (if (nil? port) -1 port) path query
              fragment)))))

(defn -normalize 
  "A Java-callable wrapper around normalize"
  [arg]
  (normalize arg))

(defn equivalent?
  "Returns true if the two URIs are equivalent when normalized.

  For example, the following two URIs are equivalent but not equal:

    http://example.com/%7b
    http://example.com/%7B"
  ([a b]
    (equivalent? a b *context*))
  ([a b context]
    (= (normalize a context) (normalize b context))))

(defn equal?
  "Returns true if the ASCII string versions of URIs are equal.  This is
  different from #'url-normalizer.core/equivalent? as two equivalent URIs
  may not have the same ASCII string representation.

  For example, the following URIs are equivalent but not equal:

    http://example.com/%7b
    http://example.com/%7B"
  [a b]
  (= (.toASCIIString #^URI (as-uri a)) (.toASCIIString #^URI (as-uri b))))

(defn with-normalization-context
  "Evaluates a function with *context* bound to context"
  [context f]
  (binding [*context* context]
    (f)))
