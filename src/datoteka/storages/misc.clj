;; Copyright (c) 2015-2017 Andrey Antukh <niwi@niwi.nz>
;; All rights reserved.
;;
;; Redistribution and use in source and binary forms, with or without
;; modification, are permitted provided that the following conditions are met:
;;
;; * Redistributions of source code must retain the above copyright notice, this
;;   list of conditions and the following disclaimer.
;;
;; * Redistributions in binary form must reproduce the above copyright notice,
;;   this list of conditions and the following disclaimer in the documentation
;;   and/or other materials provided with the distribution.
;;
;; THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
;; AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
;; IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
;; DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
;; FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
;; DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
;; SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
;; CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
;; OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
;; OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

(ns datoteka.storages.misc
  (:require [promesa.core :as p]
            [datoteka.proto :as pt]
            [datoteka.storages.local :as local])
  (:import java.nio.file.Path
           java.security.SecureRandom
           java.security.MessageDigest
           java.util.Base64))

;; --- Helpers

(defn- to-base64
  [^bytes data]
  (let [encoder (Base64/getUrlEncoder)]
    (.encodeToString encoder data)))

(defn- random-bytes
  ([^long numbytes]
   (random-bytes numbytes (SecureRandom.)))
  ([^long numbytes ^SecureRandom sr]
   (let [buffer (byte-array numbytes)]
     (.nextBytes sr buffer)
     buffer)))

(defn- sha256
  [^bytes data]
  (let [^MessageDigest md (MessageDigest/getInstance "SHA-256")]
    (.update md data)
    (.digest md)))

(defn- random-hash
  []
  (-> (random-bytes 64)
      (sha256)
      (to-base64)))

;; --- Scoped Storage

(defrecord ScopedBackend [storage ^Path prefix]
  pt/IPublicStorage
  (-public-uri [_ path]
    (let [^Path path (pt/-path [prefix path])]
      (pt/-public-uri storage path)))

  pt/IStorage
  (-save [this path content]
    (pt/-save this path content nil))
  (-save [_ path content opts]
    (let [^Path path (pt/-path [prefix path])]
      (->> (pt/-save storage path content opts)
           (p/map (fn [^Path path]
                    (.relativize prefix path))))))

  (-delete [_ path]
    (let [^Path path (pt/-path [prefix path])]
      (pt/-delete storage path)))

  (-exists? [this path]
    (let [^Path path (pt/-path [prefix path])]
      (pt/-exists? storage path)))

  (-directory? [this path]
    (let [^Path path (pt/-path [prefix path])]
      (pt/-directory? storage path)))

  (-list-dir [this path]
    (let [^Path path (pt/-path [prefix path])]
      (pt/-list-dir storage path)))

  (-create-dir [this path]
    (let [^Path path (pt/-path [prefix path])]
      (pt/-create-dir storage path)))

  (-move [this path-a path-b]
    ;; not entirely sure if the opts make much sense in the context of a
    ;; scoped storage that is not a local file system
    (pt/-move this path-a path-b #{:atomic :replace}))
  
  (-move [_ path-a path-b opts]
    (let [^Path path-a (pt/-path [prefix path-a])
          ^Path path-b (pt/-path [prefix path-b])]
      (pt/-move storage path-a path-b opts)))

  (-lookup [_ path]
    (->> (pt/-lookup storage "")
         (p/map (fn [^Path base]
                  (let [base (pt/-path [base prefix])]
                    (->> (pt/-path path)
                         (local/normalize-path base))))))))

(defn scoped
  "Create a composed storage instance that automatically prefixes
  the path when content is saved. For the rest of methods it just
  relies to the underlying storage.

  This is usefull for atomatically add sertain prefix to some
  uploads."
  [storage prefix]
  (let [prefix (pt/-path prefix)]
    (->ScopedBackend storage prefix)))

;; --- Hashed Storage

(defn- generate-path
  [^Path path]
  (let [name (str (.getFileName path))
        hash (random-hash)
        tokens (re-seq #"[\w\d\-\_]{3}" hash)
        path-tokens (take 6 tokens)
        rest-tokens (drop 6 tokens)
        path (pt/-path path-tokens)
        frest (apply str rest-tokens)]
    (pt/-path (list path frest name))))

(defrecord HashedBackend [storage]
  pt/IPublicStorage
  (-public-uri [_ path]
    (pt/-public-uri storage path))

  pt/IStorage
  (-save [_ path content]
    (let [^Path path (pt/-path path)
          ^Path path (generate-path path)]
      (pt/-save storage path content)))

  (-delete [_ path]
    (pt/-delete storage path))

  (-exists? [this path]
    (pt/-exists? storage path))

  (-lookup [_ path]
    (pt/-lookup storage path)))

(defn hashed
  "Create a composed storage instance that uses random
  hash based directory tree distribution for the final
  file path.

  This is usefull when you want to store files with
  not predictable uris."
  [storage]
  (->HashedBackend storage))

