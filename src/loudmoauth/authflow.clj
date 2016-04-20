(ns loudmoauth.authflow
  (:require [loudmoauth.util :as lmutil]
            [clojure.string :as str]
            [clojure.core.async :as a]
            [clojure.algo.generic.functor :as functor]
            [clj-http.client :as client]))

(def interaction-chan (a/chan))

(def app-state (atom {}))

(def query-params [:client-id :response-type :redirect-uri :scope :state])

(declare get-tokens)

(defn match-code-to-provider
  [params]
  (let [state (keyword (:state params))
       code (:code params)
       current-provider-data (state @app-state)]
    (deliver (:code current-provider-data) code)))

(defn query-param-string 
  "Get query-param string from query parameter map."
  [provider-auth-data]
  (->>
    (:custom-query-params provider-auth-data) (merge (select-keys provider-auth-data query-params))
    (lmutil/change-keys)
    (client/generate-query-string)))

(defn add-response-type
  "Adds response type rtype to state mape provider-auth-data"
  [rtype provider-auth-data]
  (assoc provider-auth-data :response-type rtype))

(defn add-state
  "Adds unique state-id to state map provider-auth-data."
  [provider-auth-data]
  (let [state (lmutil/uuid)]
  (assoc provider-auth-data :state state)))

(defn fetch-code
  "Fetch code to be used in call to fetch tokens."
  [provider-auth-data]
  (a/go (a/>! interaction-chan (:auth-url provider-auth-data))))

;TODO - Refactor this one.
(defn create-form-params
  "Create query-params map to include in http body."
  [provider-auth-data]
  (if-not (:refresh_token provider-auth-data)
    {:grant_type "authorization_code" 
     :code @(:code provider-auth-data)
     :redirect_uri (:redirect-uri provider-auth-data)
     :client_id (:client-id provider-auth-data)
     :client_secret (:client-secret provider-auth-data)}
    {:grant_type "refresh_token"
     :refresh_token (:refresh_token provider-auth-data)
     :client_id (:client-id provider-auth-data)
     :client_secret (:client-secret provider-auth-data)}))

(defn add-tokens-to-state-map
  "Takes state-map a state and parsed response from http request. Adds access-token and refresh-token to state map."
  [provider-auth-data parsed-body]
  (merge provider-auth-data (select-keys parsed-body [:access_token :refresh_token :expires_in])))

(defn parse-tokens
  "Parse access token and refresh-token from http response."
  [provider-auth-data]
  (->>
    (:token-response provider-auth-data)
    :body 
    (lmutil/parse-json-from-response-body)
    (add-tokens-to-state-map provider-auth-data)))

;Put this on future as we might be waiting for our promise.
(defn create-query-data
  "Creates quert data for use in http post call when retreiving tokens."
  [provider-auth-data]
  {:form-params (create-form-params provider-auth-data)})

(defn token-refresher
  "Starts a call to get-tokens in s seconds, continues forever until cancelled."
  [s]
  (future (while true (do (Thread/sleep s) (get-tokens @app-state)))))

(defn launch-token-refresher
  "Start a timed event to try to refresh oauth-tokens sometime in the future."
  [provider-auth-data]
  (when-let [token-refresher (:token-refresher provider-auth-data)]
    (future-cancel token-refresher))
  (if-let [expiry-time (:expires_in provider-auth-data)]
    (assoc provider-auth-data :token-refresher (token-refresher expiry-time))
   provider-auth-data))

;TODO If oauth-token is not set, do the initial call. If it is already set do a refresh call.
; By doing it this way we don't have to distinguish between grant_type outside get-tokens.
; We will make sure to supply a emergency refresh-token function call.
(defn get-tokens
  "Fetch tokens using crafted url" 
  [provider-auth-data]
  (->>
    (client/post (:token-url provider-auth-data) (create-query-data provider-auth-data)) 
    (assoc provider-auth-data :token-response)
    (parse-tokens)
    (launch-token-refresher)))

(defn token-url
  "Build the url for retreieving tokens."
  [provider-auth-data]
  (str (:base-url provider-auth-data) (:token-endpoint provider-auth-data)))

(defn build-token-url
  "Build token url."
  [provider-auth-data]
  (assoc provider-auth-data :token-url (token-url provider-auth-data)))

(defn auth-url
  "Build the authorization url."
  [provider-auth-data]
  (str (:base-url provider-auth-data) (:auth-endpoint provider-auth-data) "/?" (query-param-string provider-auth-data)))

(defn build-auth-url
  "Build oauth-url."
  [provider-auth-data]
  (assoc provider-auth-data :auth-url (auth-url provider-auth-data)))

(defn request-access-and-refresh-tokens
  "Request tokens."
  [provider-auth-data]
  (->>
    provider-auth-data
    (build-token-url) 
    (get-tokens)))

(defn request-access-to-data
  "Request authorization code."
  [provider-auth-data]
  (->
    provider-auth-data
    (build-auth-url)
    (fetch-code)))

(defn create-code-promise
  "Initiate promise for future code"
  [provider-auth-data]
  (assoc provider-auth-data :code (promise)))

(defn init-provider
  [provider-auth-data]
  (a/go
  (->>
    provider-auth-data
    (create-code-promise)
    (request-access-to-data)
    (request-access-and-refresh-tokens))))  

(defn init-one
  "Init one provider based on provider name."
  [provider]
  (let [provider-data (lmutil/provider-reverse-lookup provider @app-state)
        state (keyword (:state provider-data))
        ]
  (->>
  provider-data
  (init-provider)
  (swap! app-state update-in [state] @app-state))))

(defn init-all
  "Init all providers stored in app."
  [] 
  (->>
    (functor/fmap init-provider @app-state)
    (swap! app-state merge)))
