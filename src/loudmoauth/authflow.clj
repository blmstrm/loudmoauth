(ns loudmoauth.authflow
  (:require [loudmoauth.util :as lmutil]
            [clojure.string :as str]
            [clojure.core.async :as a]
            [clojure.algo.generic.functor :as functor]
            [clj-http.client :as client]))

(def interaction-chan (a/chan))

(def providers {})

(declare get-tokens)

;TODO - Deliver code
(defn match-code-to-provider
  [params]
  (let [state (keyword (:state params))
       code (:code params)
       current-provider-data (state app-state)]
    (deliver (:code current-provider-data) code)))

(defn fetch-code
  "Fetch code to be used in call to fetch tokens."
  [provider-data]
  (a/go (a/>! interaction-chan (:auth-url provider-data)))
  provider-data)

;TODO - Refactor this one.
(defn create-form-params
  "Create query-params map to include in http body."
  [provider-data]
  (print "I am checking this out!")
  (if-not (:refresh_token provider-data)
    {:grant_type "authorization_code" 
     :code @(:code provider-data)
     :redirect_uri (:redirect-uri provider-data)
     :client_id (:client-id provider-data)
     :client_secret (:client-secret provider-data)}
    {:grant_type "refresh_token"
     :refresh_token (:refresh_token provider-data)
     :client_id (:client-id provider-data)
     :client_secret (:client-secret provider-data)}))

;TODO - Swap on all three values.
(defn add-tokens-to-state-map
  "Takes state-map a state and parsed response from http request. Adds access-token and refresh-token to state map."
  [provider-data parsed-body]
  (merge provider-data (select-keys parsed-body [:access_token :refresh_token :expires_in])))

(defn parse-tokens
  "Parse access token and refresh-token from http response."
  [provider-data]
  (->>
    (:token-response provider-data)
    :body 
    (lmutil/parse-json-from-response-body)
    (add-tokens-to-state-map provider-data)))

;Put this on future as we might be waiting for our promise.
(defn create-query-data
  "Creates quert data for use in http post call when retreiving tokens."
  [provider-data]
  {:form-params (create-form-params provider-data)})

(defn token-refresher
  "Starts a call to get-tokens in s seconds, continues forever until cancelled."
  [s]
  (future (while true (do (Thread/sleep s) (get-tokens app-state)))))

(defn launch-token-refresher
  "Start a timed event to try to refresh oauth-tokens sometime in the future."
  [provider-data]
  (when-let [token-refresher (:token-refresher provider-data)]
    (future-cancel token-refresher))
  (if-let [expiry-time (:expires_in provider-data)]
    (assoc provider-data :token-refresher (token-refresher expiry-time))
    provider-data))

;TODO If oauth-token is not set, do the initial call. If it is already set do a refresh call.
; By doing it this way we don't have to distinguish between grant_type outside get-tokens.
; We will make sure to supply a emergency refresh-token function call.
(defn get-tokens
  "Fetch tokens using crafted url" 
  [provider-data]
  (->>
    (client/post (:token-url provider-data) (create-query-data provider-data)) 
    (assoc provider-data :token-response)
    (parse-tokens)
    (launch-token-refresher)))

(defn init-provider
  [provider-data]
  (->>
    provider-data
    (fetch-code)
    (get-tokens)))

(defn init-one
  "Init one provider based on provider name."
  [provider]
  (a/thread
  (let [provider-data (lmutil/provider-reverse-lookup provider providers)
        state (keyword (:state provider-data))
        ]
    (init-provider provider-data))))

(defn init-all
  "Init all providers stored in app."
  [] 
  (->>
    (functor/fmap init-provider providers)))
