(ns mochi.database
  (:require [monger.core :as mg]
            [monger.collection :as mc]
            [monger.conversion :refer [from-db-object]]
            [clojure.walk :as walk]
            [ring.util.response :refer [response]]
            [mochi.helpers :as helpers :refer [now parse-body transform-article-body-to-markdown mapply sort-articles]])

  (:import [com.mongodb MongoOptions ServerAddress])
  (:import org.bson.types.ObjectId)
  (:import [java.util Date]))


; Some Database helpers


(def get-db-uri
  "helper function to get the correct db URI"
  (or (System/getenv "MONGODB_URI")
      "mongodb://127.0.0.1:27017/blog"))

(def mongo (get (mg/connect-via-uri get-db-uri) :db))

(defn build-article-to-return
  "Build article maps to return as a response"
  [article-data]
  (-> (select-keys article-data [:created :publishedDate :isPublished :tags :title :description :_id :body])
      (mapply :_id str)))

(defn build-article-to-return-html
  "Build article maps to return as a response and transform the body as markdown"
  [article-data]
  (-> (build-article-to-return article-data)
      (mapply :body transform-article-body-to-markdown)))

(defn build-article-to-create
  [article-data]
  {:created      (now)
   :publishedDate nil
   :isPublished  false
   :tags         (get article-data :tags)
   :title        (get article-data :title)
   :description  (get article-data :description)
   :body         (get article-data :body)})

(defn build-article-to-save
  [article-data]
  (select-keys article-data [:created :publishedDate :isPublished :tags :title :description :body]))

; Functions to connect to the database

(defn find-articles
  "get all articles"
  []
  (map build-article-to-return 
       (-> (mc/find-maps mongo "articles")
           (from-db-object true)
           sort-articles)))


(defn find-articles-html
  "get all articles for html"
  []
  (map build-article-to-return-html (from-db-object (mc/find-maps mongo "articles") true)))

(defn save-document
  "Save a document to a collection in our database"
  [document]
  (mc/insert-and-return mongo "articles" document))

(defn find-document-by-id
  "Retreive a document by a id"
  [document-id]
  (build-article-to-return (from-db-object (mc/find-map-by-id mongo "articles" (ObjectId. document-id)) true)))

(defn create-article
  "Create new article"
  [request]
  (-> (build-article-to-create (parse-body request))
      (save-document)
      (build-article-to-return)
      (response)))

; mc/remove db coll {:_id oid}
(defn delete-article
  "Delete article with ID"
  [article-id]
  (mc/remove-by-id mongo "articles" (ObjectId. article-id)))

; Update a single document
; (mc/update-by-id db coll oid {:score 1088})
(defn update-article
  "update article"
  [request]
  (let [article-data (parse-body request)
        document-to-update (find-document-by-id (get-in request [:params :article-id]))
        build-pages (= (get article-data :isPublished) true)]
    (mc/update-by-id mongo "articles"
                     (ObjectId. (get-in request [:params :article-id]))
                     (->> (merge document-to-update article-data) (build-article-to-save)))
    (prn "build-pages" build-pages)
    {:build-pages build-pages}))