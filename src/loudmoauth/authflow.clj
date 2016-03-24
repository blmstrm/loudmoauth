(ns loudmoauth.authflow
  (:require [loudmoauth.util :as lmutil]
            [clojure.string :as str]
            [clojure.core.async :as a]
            [clojure.algo.generic.functor :as functor]
            [clj-http.client :as client]))

(def code-chan (a/chan))

(def interaction-chan (a/chan))

(def app-state (atom {}))

(def query-params [:client-id :response-type :redirect-uri :scope :state])

(declare get-tokens)

(defn create-headers
  "Creates headers to for use in http post call."
  [astate]
  {:Authorization (:encoded-auth-string astate)})

(defn encoded-auth-string
  "Create and encode credentials string for use in header."
  [astate]
  (str "Basic " (lmutil/string-to-base64-string (str (:client-id astate) ":" (:client-secret astate)))))

(defn query-param-string 
  "Get query-param string from query parameter map."
  [astate]
  (->>
    (:custom-query-params astate)
    (merge (select-keys astate query-params))
    (lmutil/change-keys)
    (client/generate-query-string)))

(defn add-response-type
  "Adds response type rtype to state mape astate"
  [rtype astate]
  (assoc astate :response-type rtype))

(defn add-state
  "Adds unique state-id to state map astate."
  [astate]
  (assoc astate :state (lmutil/uuid)))

(defn fetch-code
  "Fetch code to be used in call to fetch tokens."
  [astate]
  (a/go (a/>! interaction-chan (:auth-url astate)))
  (assoc astate :code (a/<!! code-chan)))

(defn add-encoded-auth-string
  "Add encoded credential string to state map."
  [astate]
  (assoc astate :encoded-auth-string (encoded-auth-string astate)))

;TODO - Refactor this one.
(defn create-form-params
  "Create query-params map to include in http body."
  [astate]
  (if-not (:refresh_token astate)
    {:grant_type "authorization_code" 
     :code (:code astate)
     :redirect_uri (:redirect-uri astate)}
    {:grant_type "refresh_token"
     :refresh_token (:refresh_token astate)}))

(defn add-tokens-to-state-map
  "Takes state-map a state and parsed response from http request. Adds access-token and refresh-token to state map."
  [astate parsed-body]
  (merge astate (select-keys parsed-body [:access_token :refresh_token :expires_in])))

(defn parse-tokens
  "Parse access token and refresh-token from http response."
  [astate]
  (->>
    (:token-response astate)
    :body 
    (lmutil/parse-json-from-response-body)
    (add-tokens-to-state-map astate)))

(defn create-query-data
  "Creates quert data for use in http post call when retreiving tokens."
  [astate]
  {:form-params (create-form-params astate)
   :headers (create-headers astate)})

(defn token-refresher
  "Starts a call to get-tokens in s seconds, continues forever until cancelled."
  [s]
  (future (while true (do (Thread/sleep s) (get-tokens @app-state)))))

(defn launch-token-refresher
  "Start a timed event to try to refresh oauth-tokens sometime in the future."
  [astate]
  (when-let [token-refresher (:token-refresher astate)]
    (future-cancel token-refresher))
  (when-let [expiry-time (:expires_in astate)]
    (assoc astate :token-refresher (token-refresher expiry-time))))

;TODO If oauth-token is not set, do the initial call. If it is already set do a refresh call.
; By doing it this way we don't have to distinguish between grant_type outside get-tokens.
; We will make sure to supply a emergency refresh-token function call.
(defn get-tokens
  "Fetch tokens using crafted url" 
  [astate]
  (->>
    (client/post (:token-url astate) (create-query-data astate)) 
    (assoc astate :token-response)
    (parse-tokens)
    (launch-token-refresher)))

(defn token-url
  "Build the url for retreieving tokens."
  [astate]
  (str (:base-url astate) "/api/token"))

(defn build-token-url
  "Build token url."
  [astate]
  (assoc astate :token-url (token-url astate)))

(defn auth-url
  "Build the authorization url."
  [astate]
  (str (:base-url astate) "/authorize/?" (query-param-string astate)))

(defn build-auth-url
  "Build oauth-url."
  [astate]
  (assoc astate :auth-url (auth-url astate)))

(defn request-access-and-refresh-tokens
  "Request tokens."
  [state-map]
  (->>
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

(defn init-provider
  [provider-auth-data]
  (->>
    provider-auth-data
    (request-access-to-data)
    (request-access-and-refresh-tokens)))  

(defn init-one
  "Init one provider based on provider name."
  [provider]
  (->>
  (init-provider (provider @app-state))
  (swap! app-state merge)))

(defn init-all
  "Init all providers stored in app."
  [] 
  (->>
    (functor/fmap init-provider @app-state)
    (swap! app-state merge)))
