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

(ns datoteka.proto
  "A storage abstraction definition.")

(defprotocol IUri
  (-uri [_] "Coerce to uri."))

(defprotocol IPath
  (-path [_] "Coerce to path."))

(defprotocol IContent
  (-input-stream [_] "Coerce to input stream."))

(defprotocol IStorage
  "A basic abstraction for storage access."
  (-lookup [_ path] "Resolves the path to the local filesystem.")
  (-save [_ path content] [_ path content opts] "Persist the content under specified path.")
  (-delete [_ path] "Delete the file/directory by its path.")
  (-move [_ path-a path-b] [_ path-a path-b opts] "Move the file from a to b")
  (-exists? [_ path] "Check if file exists by path.")
  (-directory? [_ path] "Is this a directory?")
  (-list-dir [_ path] "List the directory")
  (-create-dir [_ path] "Create the directory"))

(defprotocol IClearableStorage
  (-clear [_] "clear all contents of the storage"))

(defprotocol IPublicStorage
  (-public-uri [_ path] "Get a public accessible uri for path."))

(defprotocol IStorageIntrospection
  (-accessed-time [_ path] "Return the last accessed time of the file.")
  (-created-time [_ path] "Return the creation time of the file.")
  (-modified-time [_ path] "Return the last modified time of the file."))

