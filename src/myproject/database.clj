(ns myproject.database
  (:require [monger.core :as mg]
            [monger.collection :as mc]
            [monger.conversion :refer [from-db-object]]
            [clojure.walk :as walk]
            [myproject.helpers :as helpers :refer [now parse-body transform-article-body-to-markdown]])

  (:import [com.mongodb MongoOptions ServerAddress])
  (:import org.bson.types.ObjectId)
  (:import [java.util Date]))


; Some Database helpers


(defn get-db-uri
  "helper function to get the correct db URI"
  []
  (or (System/getenv "MONGODB_URI")
      "mongodb://127.0.0.1:27017"))

(defn build-article-to-return
  [article-data]
  {:id           (str (get article-data :_id))
   :created      (get article-data :created)
   :publishedDate (get article-data :publishedDate)
   :isPublished  (get article-data :isPublished)
   :tags         (get article-data :tags)
   :title        (get article-data :title)
   :description  (get article-data :description)
   :body         (get article-data :body)})

(defn build-article-to-return-html
  [article-data]
  {:id           (str (get article-data :_id))
   :created      (get article-data :created)
   :publishedDate (get article-data :publishedDate)
   :isPublished  (get article-data :isPublished)
   :tags         (get article-data :tags)
   :title        (get article-data :title)
   :description  (get article-data :description)
   :body         (transform-article-body-to-markdown (get article-data :body))})

(defn build-article-to-save
  [article-data]
  {:created      (now)
   :publishedDate nil
   :isPublished  false
   :tags         (get article-data :tags)
   :title        (get article-data :title)
   :description  (get article-data :description)
   :body         (get article-data :body)})

; Functions to connect to the database

(defn find-articles
  "get all articles"
  []
  (let [uri (get-db-uri)
        {:keys [conn db]} (mg/connect-via-uri uri)
        coll "articles"]
    (map build-article-to-return (from-db-object (mc/find-maps db coll) true))))

(defn find-articles-html
  "get all articles for html"
  []
  (let [uri (get-db-uri)
        {:keys [conn db]} (mg/connect-via-uri uri)
        coll "articles"]
    (map build-article-to-return-html (from-db-object (mc/find-maps db coll) true))))

(defn save-document
  "Save a document to a collection in our database"
  [document]
  (let [uri (get-db-uri)
        {:keys [conn db]} (mg/connect-via-uri uri)]
    (mc/insert-and-return db "articles" document)))

(defn find-document-by-id
  "Retreive a document by a id"
  [document-id]
  (let [uri (get-db-uri)
        {:keys [conn db]} (mg/connect-via-uri uri)
        coll "articles"]
    (from-db-object (mc/find-map-by-id db coll (ObjectId. document-id)) true)))

(defn create-article
  "Create new article"
  [request]
  (-> (build-article-to-save (parse-body request))
      (save-document)))

; mc/remove db coll {:_id oid}
(defn delete-article
  "Delete article with ID"
  [article-id]
  (let [uri (get-db-uri)
        {:keys [conn db]} (mg/connect-via-uri uri)
        coll "articles"]
    (mc/remove-by-id db coll (ObjectId. article-id))))

; Update a single document
; (mc/update-by-id db coll oid {:score 1088})
(defn update-article
  "update article"
  [request]
  (let [uri (get-db-uri)
        {:keys [conn db]} (mg/connect-via-uri uri)
        coll "articles"
        article-data (parse-body request)
        document-to-update (find-document-by-id (get article-data :id))
        build-pages (= (get article-data :isPublished) true)]
    (mc/update-by-id db coll
                     (ObjectId. (get article-data :id))
                     (merge document-to-update article-data))
    (prn "build-pages" build-pages)
    {:build-pages build-pages}))