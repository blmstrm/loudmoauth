(ns loudmoauth.core
  (:require [clojure.string :as str]
            [clojure.core.async :as a]
            [clojure.data.codec.base64 :as b64]
            [clojure.data.json :as json]
            [clj-http.util :as util]
            [clj-http.client :as client]))

;TODO - Deal with errors and exceptions
;TODO - Deal with missing :redirect-uri key, if that is possible?
;TODO - Go over doc strings one more time.
;TODO - Uniform keys all over.
;TODO - Crap, we need to let the user authenticate. Don't put the get auth code on a future.
;       Our solution is to put the code url on a channel and let user redirect to that channel in his/her implementation, or what ever they like to do to make the process proced.

(declare get-tokens)

(def code-chan (a/chan))

(def interaction-chan (a/chan))

(def app-state (atom {}))

(defn uuid [] (str (java.util.UUID/randomUUID)))

(def query-params [:client-id :response-type :redirect-uri :scope :state])

;TODO - Should omit bringing in the vals here?
(defn change-keys 
  "Change hyphen to underscore in param map keys."
  [params]
  (if-not params
    nil
    (let [ks (keys params)
          vs (vals params)
          new-keys (map #(keyword (str/replace (name %) "-" "_")) ks)]
      (zipmap new-keys vs))))

(defn query-param-string 
  "Get query-param string from query parameter map."
  [astate]
  (->>
    (:custom-query-params astate)
    (merge (select-keys astate query-params))
    (change-keys)
    (client/generate-query-string)))

(defn add-response-type
  "Adds response type rtype to state mape astate"
  [rtype astate]
  (assoc astate :response-type rtype))

(defn add-state
  "Adds unique state-id to state map astate."
  [astate]
  (assoc astate :state (uuid)))

;This is the handlers function to call after receiving callback to specified url.
;TODO - Deal with cases where token is nil. If so don't put on channel.
(defn parse-code
  "Parse code from http-response and deliver promise."
  [response]
  (println "Trying to parse code.")
  (a/go
    (->>
      response 
      :params
      :code 
      (a/>! code-chan))))

(defn fetch-code
  "Fetch code to be used in call to fetch tokens."
  [astate]
  (a/go (a/>! interaction-chan (:auth-url astate)))
  (assoc astate :code (a/<!! code-chan)))

;This is the handlers function to retrieve different kinds of dialog pages, nescessary for identification.
(defn user-interaction
  "Prompt for user interaction."
  [request]
  (if-let [interaction-url (a/poll! interaction-chan)]
    interaction-url
    "No user interaction nescessary."))

(defn string-to-base64-string
  "b64 encode string"
  [original]
  (String. (b64/encode (.getBytes original)) "UTF-8"))

(defn encoded-auth-string
  "Create and encode credentials string for use in header."
  [astate]
  (str "Basic " (string-to-base64-string (str (:client-id astate) ":" (:client-secret astate)))))

(defn add-encoded-auth-string
  "Add encoded credential string to state map."
  [astate]
  (assoc astate :encoded-auth-string (encoded-auth-string astate)))

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

(defn parse-json-from-response-body
  "Parse json in response body"
  [response-body]
  (json/read-str response-body :key-fn keyword))

(defn parse-tokens
  "Parse access token and refresh-token from http response."
  [astate]
  (println "Trying to parse tokens.")
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
  (println "Fetching tokens with post from custom url.")
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
  (println "Starting in request-access-and-refresh-tokens.")
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
      (if-not (:response-type params)
        (add-response-type "code" params)
        params)
      (add-state) 
      (merge old-app-state)
      (add-encoded-auth-string)
      (reset! app-state))))

(defn oauth-token
  "Retreive oauth token for use in authentication call"
  []
  (:access_token @app-state))
