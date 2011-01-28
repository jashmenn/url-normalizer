(ns url-normalizer.util
  (:require
    [clojure.contrib.str-utils2 :as su]))

(defn byte-to-hex-string
  "Converts the lower 16 bits of b to into a hex string"
  [b]
  (let [s (Integer/toHexString (bit-and 0xff b))]
    (if (= (count s) 1) (str "0" s) s)))

(def alpha
  (let [xs (concat (range 0x41 0x5A) (range 0x61 0x7A))]
    (zipmap (map #(str "%" (su/upper-case (byte-to-hex-string %))) xs)
            (map #(str (char %)) xs))))

(def digits
  (let [xs (range 0x30 0x39)]
    (zipmap (map #(str "%" (byte-to-hex-string %)) xs)
            (map #(str (char %)) xs))))

(def decode-alphanum
  (concat
    (map #(fn [s] (.replaceAll s (first %) (last %))) alpha)
    (map #(fn [s] (.replaceAll s (first %) (last %))) digits)))