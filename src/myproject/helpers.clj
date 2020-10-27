(ns myproject.helpers
  (:import [java.util Date])
  (:require [markdown.core :as md]))

(defn now [] (Date.))

(defn parse-body
  "Get the body of a request. This takes the payload"
  [request]
  (get request :body))

(defn transform-article-body-to-markdown
  "transform the body of an article to markdown"
  [body]
  (md/md-to-html-string body))