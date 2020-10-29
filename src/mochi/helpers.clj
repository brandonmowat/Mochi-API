(ns mochi.helpers
  (:import [java.util Date])
  (:require [markdown.core :as md]))

(defn now [] (Date.))

(defn parse-body
  "Get the body of a request. This takes the payload"
  [request]
  (get request :body))

(def transform-article-body-to-markdown
  "transform the body of an article to markdown"
  md/md-to-html-string)

(defn mapply
  "Apply the function to a keys value in a map"
  [map key fn]
  (assoc map key (fn (get map key))))