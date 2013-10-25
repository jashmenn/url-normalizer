DESCRIPTION
===========

Normalize URLs, with options for selecting normalizations that may or may not be semantic preserving.

These normalizations are only tested well against the HTTP and HTTPS schemes.


SEMANTIC PRESERVING NORMALIZATIONS
==================================

Applying these normalizations will not cause the URL to describe a different resource.

* Lower case the scheme portion
* Lower case the host
* Upper case percent encodings
* Decode unreserved characters
* Add a trailing slash to the host
* Remove the default port
* Remove dot segments

NON SEMANTIC PRESERVING NORMALIZATIONS
======================================

Apply these normalizations with caution because technically they cause the URL to describe a different resource (but sometimes the server won't care; e.g. if you remove the fragment).  There is some trickiness involved with hashbang segments for sites like Twitter, so there is an option for that.

* Remove the directory index
* Remove the fragment
* Convert from an IP address to a hostname
* Remove duplicate query keys and values
* Remove empty query
* Remove empty user info segment
* Remove trailing dot in host
* Keep the hashbang fragment
* Force http instead of https
* Remove the www from the host
* Sort the query keys
* Decode reserved characters

BUILDING
========

Make sure to delete your classes and lib directory if you are upgrading.  Leiningen and Clojure are finicky.

USAGE
=====

Use the `normalize` function to apply specific normalizations to URLs.  Note that only safe, semantic preserving normalizations are applied by default.

    (use '[url-normalizer.core :exclude (resolve)])

    (normalize "http://WWW.EXAMPLE.COM:80/%7ejane/foo/bar/../baz")
    -> #<URI http://www.example.com/~jane/foo/baz>

    (normalize "../../../../bif#foo" {:base "http://example.com:8080/a/b/c/f/d"})
    -> #<URI http://example.com:8080/a/bif#foo>

    (normalize "http://example.com?")
    -> #<URI http://example.com/?>

    (normalize "http://example.com?" {:remove-empty-query? true})
    -> #<URI http://example.com/>

    (normalize "http://example.com/#!/foo" {:remove-fragment? true})
    -> #<URI http://example.com/>

    (normalize "http://example.com/#!/foo" {:remove-fragment? true
                                            :keep-hashbang-fragment? true})
    -> #<URI http://example.com/#!/foo>

    (normalize "http://例え.テスト/")
    -> #<URI http://xn--r8jz45g.xn--zckzah/>

    (with-normalization-context
      {:lower-case-host? false
       :remove-default-port? false
       :remove-empty-query? true
       :remove-trailing-dot-in-host? true}
     #(normalize "http://WWW.example.COM.:80/?"))
    -> #<URI http://WWW.example.COM:80/>

See `#'url-normalizer.core/*context*` for applicable normalizations.  Some normalizations do not preserve semantics; be warned.

You can also test if two URLs are equivalent or if they are equal.  Two URLs are equivalent if they normalize to the same thing, but they are equal if their ascii representations are the same.

    (use 'url-normalizer.core)

    (equivalent? "http://example.com" "http://example.com/")
    -> true

    (equal? "http://example.com" "http://example.com/")
    -> false

AUTHORS
=======

* Min Huang [<min.huang@alumni.usc.edu>](mailto:min.huang@alumni.usc.edu)
* Jay Donnell
* Nate Murray [<nate@xcombinator.com>](mailto:nate@xcombinator.com)

Tests taken from Sam Ruby's version of `urlnorm.py`

SEE ALSO
========

* Sam Ruby's [`urlnorm.py`](http://intertwingly.net/blog/2004/08/04/Urlnorm)
* [Wikipedia Page on URL Normalization](http://en.wikipedia.org/wiki/URL_normalization)
* [Sang Ho Lee, Sung Jin Kim, and Seok Hoo Hong (2005). "On URL normalization". Proceedings of the International Conference on Computational Science and its Applications (ICCSA 2005). pp. 1076–1085.](http://dblab.ssu.ac.kr/publication/LeKi05a.pdf)
* [RFC3986](http://www.ietf.org/rfc/rfc3986.txt)
* [RFC2396](http://labs.apache.org/webarch/uri/rev-2002/rfc2396bis.html)
* [RFC1808](http://www.ietf.org/rfc/rfc1808.txt)

LICENSE
=======

Copyright (C) 2010

Distributed under the Eclipse Public License, the same as Clojure.
