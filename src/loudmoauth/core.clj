(ns loudmoauth.core
  (:require [clojure.string :as str]
            [clojure.core.async :as a]
            [clojure.data.codec.base64 :as b64]
            [clojure.data.json :as json]
            [clj-http.util :as util]
            [clj-http.client :as client]))

;TODO - Deal with errors and exceptions
;TODO - Deal with missing :redirect-uri key, if that is possible?
;TODO - Make the oauth-params map accept keys on this format :client-id instead of :client_id.

(def code-chan (a/chan))

(def app-state (atom {}))

(defn uuid [] (str (java.util.UUID/randomUUID)))

(def query-params [:client_id :response_type :redirect_uri :scope :state])

(defn query-param-string 
  "Get query-param string from query parameter map."
  [astate]
  (->>
    (:custom-query-params astate)
    (merge (select-keys astate query-params))
    (client/generate-query-string)))

(defn add-response-type
  "Adds response type rtype to state mape astate"
  [rtype astate]
  (assoc astate :response_type rtype))

(defn add-state
  "Adds unique state-id to state map astate."
  [astate]
  (assoc astate :state (uuid)))

;This is the handlers function to call after receiving callback to specified url.
(defn parse-code
  "Parse code from http-response and deliver promise."
  [response]
  (a/go
    (->>
      response
      :params
      :code
      (a/>! code-chan))))

(defn fetch-code
  "Fetch code to be used in call to fetch tokens."
  [astate]
  (future (client/get (:auth-url astate)))
  (assoc astate :code (a/<!! code-chan)))

(defn string-to-base64-string
  "b64 encode string"
  [original]
  (String. (b64/encode (.getBytes original)) "UTF-8"))

(defn encoded-auth-string
  "Create and encode credentials string for use in header."
  [astate]
  (str "Basic " (string-to-base64-string (str (:client_id astate) ":" (:client_secret astate)))))

(defn add-encoded-auth-string
  "Add encoded credential string to state map."
  [astate]
  (assoc astate :encoded-auth-string (encoded-auth-string astate)))

(defn create-body-params
  "Create query-params map to include in http body."
  [astate]
  {:grant_type "authorization_code" 
   :code (:code astate)
   :redirect_uri (:redirect_uri astate)})

(defn add-tokens-to-state-map
  "Takes state-map a state and parsed response from http request. Adds access-token and refresh-token to state map."
  [astate parsed-body]
  (merge astate (select-keys parsed-body [:access_token :refresh_token :expires_in])))

(defn parse-json-from-response-body
  "Parse json in response body"
  [response-body]
  (json/read-str response-body :key-fn keyword))

(defn parse-tokens
  "Parse access token and refresh-token from http response."
  [astate]
  (->>
    (:token-response astate)
    :body 
    (parse-json-from-response-body)
    (add-tokens-to-state-map astate)))

(defn create-headers
  "Creates headers to for use in http post call."
  [astate]
  {:Authorization (:encoded-auth-string astate)})

(defn create-query-data
  "Creates quert data for use in http post call when retreiving tokens."
  [astate]
  {:body (create-body-params astate)
   :headers (create-headers astate)})

(defn get-tokens
  "Fetch tokens using crafted url" 
  [astate]
  (->>
    (client/post (:token-url astate) (create-query-data astate)) 
    (assoc astate :token-response)
    (parse-tokens)))

(defn token-url
  "Build the url for retreieving tokens."
  [astate]
  (str (:base_url astate) "/api/token"))

(defn build-token-url
  "Build token url."
  [astate]
  (assoc astate :token-url (token-url astate)))

(defn auth-url
  "Build the authorization url."
  [astate]
  (str (:base_url astate) "/authorize/?" (query-param-string astate)))

(defn build-auth-url
  "Build oauth-url."
  [astate]
  (assoc astate :auth-url (auth-url astate)))

(defn request-access-and-refresh-tokens
  "Request tokens."
  [state-map]
  (->
    state-map
    (build-token-url) 
    (get-tokens)))

(defn request-access-to-data
  "Request authorization code."
  [astate]
  (->
    astate
    (build-auth-url)
    (fetch-code)))

(defn init
  "Init ouath token request cycle."
  []
  (let [ old-app-state @app-state] 
  (->>
    old-app-state
    (request-access-to-data)
    (request-access-and-refresh-tokens)
    (reset! app-state))))

(defn set-oauth-params
  "Set oauth-parameters for use in call to get token"
  [params]
  (let [old-app-state @app-state]
  (->>
    (if-not (:response_type params)
      (add-response-type "code" params)
      params)
    (add-state) 
    (merge old-app-state)
    (add-encoded-auth-string)
    (reset! app-state))))

(defn oauth-token
  "Retreive oauth token for use in authentication call"
  []
  (when-not (:access_token @app-state) (init))
  (:access_token @app-state))
