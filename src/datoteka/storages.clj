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

(ns datoteka.storages
  "A datoteka abstraction layer."
  (:require [datoteka.proto :as pt]))

(defn save
  "Perists a file or bytes in the storage. This function
  returns a relative path where file is saved.

  The final file path can be different to the one provided
  to this function and the behavior is totally dependen on
  the storage implementation."
  ([storage path content]
   (pt/-save storage path content))
  ([storage path content opts]
   (pt/-save storage path content opts)))

(defn lookup
  "Resolve provided relative path in the storage and return
  the local filesystem absolute path to it.
  This method may be not implemented in all datoteka."
  [storage path]
  (pt/-lookup storage path))

(defn move
  "Move from `path a` to `path b` within the storage"
  ([storage path-a path-b]
   (pt/-move storage path-a path-b))
  ([storage path-a path-b opts]
   (pt/-move storage path-a path-b opts)))

(defn exists?
  "Check if a  relative `path` exists in the storage."
  [storage path]
  (pt/-exists? storage path))

(defn directory?
  "Check if a `path` in the storage is a directory"
  [storage path]
  (pt/-directory? storage path))

(defn list-dir
  "Try to list a `path` as a directory in the storage"
  [storage path]
  (pt/-list-dir storage path))

(defn create-dir
  "Try to create a directory given a `path` in the storage"
  [storage path]
  (pt/-create-dir storage path))

(defn delete
  "Delete a `path` from the storage."
  [storage path]
  (pt/-delete storage path))

(defn clear!
  "Clear all contents of the storage."
  [storage]
  (pt/-clear storage))

(defn public-url
  [storage path]
  (pt/-public-uri storage path))

(defn storage?
  "Return `true` if `v` implements IStorage protocol"
  [v]
  (satisfies? pt/IStorage v))
