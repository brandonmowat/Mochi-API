(ns mochi.core
  (:require [ring.middleware.defaults :refer [site-defaults wrap-defaults]]
            [ring.middleware.reload :refer [wrap-reload]]
            [ring.middleware.json :refer [wrap-json-body wrap-json-response]]
            [ring.middleware.cors :refer [wrap-cors]]
            [ring.util.response :refer [response]]
            [ring.middleware.basic-authentication :refer [wrap-basic-authentication]]
            [clj-http.client :as client]

            [compojure.core :refer :all]
            [compojure.route :as route]
            [org.httpkit.server :refer [run-server]]
            [clojure.java.io :as io]


            ;; Relative dependencies


            [mochi.database :as db :refer
             [find-document-by-id find-articles find-articles-html create-article delete-article update-article]])
  (:gen-class))

(defn authenticated? [name pass]
  (and (= name (System/getenv "ADMIN_USERNAME"))
       (= pass (System/getenv "ADMIN_PASSWORD"))))

(defn get-port
  "helper function to get the correct port"
  []
  (-> (or (System/getenv "PORT")
          "5000")
      (Integer/parseInt)))

(defn trigger-blog-build
  "trigger a deploy on netlify. This will rebuild that static files"
  [build-pages]
  (if (not= (get build-pages :build-pages) false) (client/post (System/getenv "BUILD_HOOK_URL") {})))

(defn retrieve-article [post-id]
  (prn post-id)
  (find-document-by-id post-id))

(defn get-articles
  "Get all articles"
  [request]
  (response (find-articles)))

(defn get-articles-html
  "Get all articles"
  [request]
  (response (find-articles-html)))

(defn delete-articles-request-handler
  "delete article request handler"
  [article-id]
  (delete-article article-id)
  {:status 200})

(defn patch-article-request-handler
  "Patch an article. If it indicates that we should trigger a build, build static pages."
  [request]
  (->> (update-article request)
       (trigger-blog-build))
  (response {:status 200 :build true}))

;; Public Routes
(defroutes public-routes*
  (GET "/articles/" []
    (wrap-json-response get-articles-html)))

(def public-routes
  (-> #'public-routes*))

;; private routes
(defroutes admin-routes*
  ;; (GET "/" [] (slurp (io/resource "public/index.html")))
  (context "/api/v1/articles" []
    (GET "/" []
      (wrap-json-response get-articles))
    (POST "/" [] (wrap-json-body create-article {:keywords? true})))
  (context "/api/v1/articles/:article-id" [article-id]
    (GET "/" [] (retrieve-article article-id))
    (POST "/" [] (wrap-json-response (wrap-json-body patch-article-request-handler {:keywords? true})))
    (DELETE "/" [] (delete-articles-request-handler article-id))))

(def admin-routes
  (-> #'admin-routes*
      (wrap-basic-authentication authenticated?)))

(defroutes project-api
  (ANY "*" [] public-routes)
  (ANY "*" [] admin-routes))

(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (prn (System/getenv))
  (prn 'STARTING-SERVER "on port" (str (get-port)))
  (def stop-server
    (-> (wrap-reload #'project-api)
        (wrap-defaults (assoc-in site-defaults [:security :anti-forgery] false))
        (wrap-cors  :access-control-allow-credentials "true"
            :access-control-allow-origin #".*"
            :access-control-allow-methods [:get :post :delete :patch])
        (run-server {:port (get-port)}))))
