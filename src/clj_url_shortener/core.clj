(ns clj-url-shortener.core
    (:gen-class)
    (:require [compojure.route :as route]
      [ring.util.response :as response]
      [ring.middleware.defaults :as defaults])
    (:use compojure.core
      ring.adapter.jetty)
    (:import
      org.apache.commons.validator.routines.UrlValidator))

;; app-runner configuration
(def app-port (Integer/parseInt (get (System/getenv) "APP_PORT" "3000")))
(def app-name (get (System/getenv) "APP_NAME" "clj-url-shortener"))
(def app-data (get (System/getenv) "APP_DATA" "target"))
(def data-file (str app-data (java.io.File/separator) "clj-url-shortener.data"))
(def context-path (str "/" app-name))

(def validator (UrlValidator. (into-array ["http" "https"]) (. UrlValidator ALLOW_LOCAL_URLS)))

;; the state stuff

(def urls (atom {}))

(def id-generator
  (atom 0))

(defn next-id []
  (swap! id-generator inc))

(defn store [token url]
  (swap! urls assoc url token)
  (spit data-file @urls)
  token)

(defn load-state
  []
  (try
    (let [loaded-urls (read-string (slurp data-file))
          max-val (-> loaded-urls
                      vals
                      sort
                      last
                      Integer.)]
      (reset! urls loaded-urls)
      (reset! id-generator max-val))
    (catch Exception e
      (println e))))

;; main functions

(defn validToken?
  [token]
  (try
    (if (Long. token)
      true
      false)
    (catch Exception e
      false))
  )

(defn shorten [params request]
  (let [url (:url params)
        token (get @urls url)
        short-url-base (str "http://" (:server-name request) ":" (:server-port request) (:context request) "/x/")]
    (if (validToken? token)
      (str short-url-base token)
      (if (.isValid validator url)
        (do
          (let [new_token (next-id)]
            (store (str new_token) url)
            (str short-url-base new_token)))
        (str "invalid url: " url)
        )
      )
    )
  )

(defn expand [token]
  (let [url (get (clojure.set/map-invert @urls) (str token))]
    (if-not (= nil url)
      (response/redirect url)
      {:status  404
       :headers {"Content-Type" "text/plain"}
       :body    (str "URL with token: " token " not found")}
      )
    )
  )

(defroutes
  main-routes
  (GET "/" [] (response/redirect context-path))
  (context context-path []
           (GET "/" [] (response/redirect (str context-path "/index.html")))
           (GET "/s" {params :params :as request} (shorten params request))
           (GET "/x/:code"  [code] (expand code))
           (route/resources "/")
           (route/not-found "Page not found")))

(def app
  (defaults/wrap-defaults main-routes defaults/api-defaults))

(defn -main []
      (println "State loaded from file")
      (load-state)
      (println "Server started at " (str "http://localhost:" app-port context-path))
      (run-jetty app {:port app-port}))
