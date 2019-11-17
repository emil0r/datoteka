;; Copyright (c) 2015-2019 Andrey Antukh <niwi@niwi.nz>
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

(ns datoteka.storages.local
  "A local filesystem storage implementation."
  (:require [promesa.core :as p]
            [clojure.java.io :as io]
            [datoteka.proto :as pt]
            [datoteka.core :as fs])
  (:import java.io.InputStream
           java.io.OutputStream
           java.net.URI
           java.nio.file.Path
           java.nio.file.Files
           java.nio.file.NoSuchFileException
           java.util.function.Supplier
           java.util.concurrent.CompletableFuture
           java.util.concurrent.ExecutorService
           java.util.concurrent.ForkJoinPool))

(defn normalize-path
  [^Path base ^Path path]
  (if (fs/absolute? path)
    (throw (ex-info "Suspicios operation: absolute path not allowed."
                    {:path (str path)}))
    (let [^Path fullpath (.resolve base path)
          ^Path fullpath (.normalize fullpath)]
      (when-not (.startsWith fullpath base)
        (throw (ex-info "Suspicios operation: go to parent dir is not allowed."
                        {:path (str path)})))
      fullpath)))

(defn- save
  [base path content overwrite?]
  (let [^Path path (pt/-path path)
        ^Path fullpath (normalize-path base path)]
    (when-not (fs/exists? (.getParent fullpath))
      (fs/create-dir (.getParent fullpath)))
    (if overwrite?

      (with-open [^InputStream src (pt/-input-stream content)
                  ^OutputStream dst (io/output-stream fullpath)]
        (io/copy src dst)
        (fs/relativize fullpath base))
      
      (loop [iteration nil]
        (let [[basepath ext] (fs/split-ext fullpath)
              candidate (fs/path (str basepath iteration ext))]
          (if (fs/exists? candidate)
            (recur (if (nil? iteration) 1 (inc iteration)))
            (with-open [^InputStream src (pt/-input-stream content)
                        ^OutputStream dst (io/output-stream candidate)]
              (io/copy src dst)
              (fs/relativize candidate base))))))))

(defn- delete
  [base path]
  (let [path (->> (pt/-path path)
                  (normalize-path base))]
    (try
      (fs/delete path)
      true
      (catch java.nio.file.NoSuchFileException e
        false))))

(defn- submit
  [executor func]
  (let [supplier (reify Supplier (get [_] (func)))]
     (CompletableFuture/supplyAsync supplier executor)))

(defrecord LocalFileSystemBackend [^Path base
                                   ^URI baseuri
                                   ^ExecutorService executor
                                   ^Boolean overwrite?]
  pt/IPublicStorage
  (-public-uri [_ path]
    (.resolve baseuri (str path)))

  pt/IStorage
  (-save [_ path content]
    (submit executor #(save base path content overwrite?)))
  (-save [_ path content opts]
    (submit executor #(save base path content (:overwrite? opts))))

  (-delete [_ path]
    (submit executor #(delete base path)))

  (-exists? [this path]
    (try
      (p/resolved
       (let [path (->> (pt/-path path)
                       (normalize-path base))]
         (fs/exists? path)))
      (catch Exception e
        (p/rejected e))))
  
  (-directory? [this path]
    (try
      (p/resolved
       (let [path (->> (pt/-path path)
                       (normalize-path base))]
         (fs/directory? path)))
      (catch Exception e
        (p/rejected e))))

  (-list-dir [this path]
    (try
      (p/resolved
       (let [path (->> (pt/-path path)
                       (normalize-path base))]
         (fs/list-dir path)))
      (catch Exception e
        (p/rejected e))))

  (-create-dir [this path]
    (try
      (p/resolved
       (let [path (->> (pt/-path path)
                       (normalize-path base))]
         (fs/create-dir path)))
      (catch Exception e
        (p/rejected e))))

  (-move [this path-a path-b]
    (pt/-move this path-a path-b #{:atomic :replace}))

  (-move [this path-a path-b opts]
    (try
      (p/resolved
       (let [path-a (->> (pt/-path path-a)
                         (normalize-path base))
             path-b (->> (pt/-path path-b)
                         (normalize-path base))]
         (fs/move path-a path-b opts)))
      (catch Exception e
        (p/rejected e))))

  (-lookup [_ path']
    (try
      (p/resolved
       (->> (pt/-path path')
            (normalize-path base)))
      (catch Exception e
        (p/rejected e))))

  pt/IClearableStorage
  (-clear [_]
    (fs/delete base)
    (fs/create-dir base)))


(defn localfs
  "Create an instance of local FileSystem storage providing an
  absolute base path.

  If that path does not exists it will be automatically created,
  if it exists but is not a directory, an exception will be
  raised.

  This function expects a map with the following options:
  - `:basedir`: a fisical directory on your local machine
  - `:baseuri`: a base uri used for resolve the files
  - `:executor`: an executor instance (optional)
  "
  [{:keys [basedir baseuri executor overwrite?]
    :or {executor (ForkJoinPool/commonPool)
         overwrite? false}
    :as options}]
  (let [^Path basepath (pt/-path basedir)
        ^URI baseuri (pt/-uri baseuri)]
    (when (and (fs/exists? basepath)
               (not (fs/directory? basepath)))
      (throw (ex-info "File already exists." {})))

    (when-not (fs/exists? basepath)
      (fs/create-dir basepath))

    (->LocalFileSystemBackend basepath baseuri executor overwrite?)))
