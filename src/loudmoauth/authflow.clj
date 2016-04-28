(ns loudmoauth.authflow
  (:require [loudmoauth.util :as lmutil]
            [clojure.string :as str]
            [clojure.core.async :as a]
            [clj-http.client :as client]))

(def interaction-chan (a/chan))

(def providers {})

(declare get-tokens)

(defn match-code-to-provider
  [params]
  (let [state (keyword (:state params))
        code (:code params)
        current-provider-data (state providers)]
    (deliver (:code current-provider-data) code)))

(defn fetch-code!
  "Fetch code to be used in call to fetch tokens."
  [auth-url]
  (a/go (a/>! interaction-chan auth-url)))

(defn create-form-params
  "Create query-params map to include in http body."
  [provider-data]
  (merge
    {:client_id (:client-id provider-data)
     :client_secret (:client-secret provider-data)
     }
    (if-not (:refresh_token provider-data)
      {:grant_type "authorization_code" 
       :code @(:code provider-data)
       :redirect_uri (:redirect-uri provider-data)}
      {:grant_type "refresh_token"
       :refresh_token (:refresh_token provider-data)})))

(defn add-tokens-to-provider-data
  "Takes state-map a state and parsed response from http request. Adds access-token and refresh-token to state map."
  [provider-data parsed-body]
  (swap! (:token-data provider-data) merge (select-keys parsed-body [:access_token :refresh_token :expires_in])))

(defn parse-tokens
  "Parse access token and refresh-token from http response."
  [provider-data]
  (->>
    (:token-response provider-data)
    :body 
    (lmutil/parse-json-from-response-body)
    (add-tokens-to-provider-data provider-data)))

(defn create-query-data
  "Creates quert data for use in http post call when retreiving tokens."
  [provider-data]
  {:form-params (create-form-params provider-data)})

(defn token-refresher
  "Starts a call to get-tokens in s seconds, continues forever until cancelled."
  [s provider-data]
  (future (while true (do (Thread/sleep s) (get-tokens provider-data)))))

;TODO - What if when and if-let below all resolve to false? We will break our function chain.
(defn launch-token-refresher
  "Start a timed event to try to refresh oauth-tokens sometime in the future."
  [provider-data]
  (when-let [token-refresher (:token-refresher provider-data)]
    (future-cancel token-refresher))
  (if-let [expiry-time (:expires_in provider-data)]
    (assoc provider-data :token-refresher (token-refresher expiry-time provider-data))
    provider-data))

(defn get-tokens
  "Fetch tokens using crafted url" 
  [provider-data]
  (->>
    (client/post (:token-url provider-data) (create-query-data provider-data)) 
    (assoc provider-data :token-response)
    (parse-tokens))
  (launch-token-refresher provider-data))

(defn init-and-add-provider
  [provider-data]
  (fetch-code! (:auth-url provider-data))
  (->>
    provider-data
    (get-tokens)
    (assoc providers (:state provider-data))))
