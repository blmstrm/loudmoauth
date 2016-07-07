(ns loudmoauth.core
  (:require [loudmoauth.authflow :as lma]
            [loudmoauth.provider :as p])) 

;Use channel here? Calling a function like this is so nesting.
(defn parse-params
  "Parse parameters from http-response and put on channel."
  [response]
    (->>
      response 
      :params
      (lma/match-code-to-provider)))

;Reverser match on provider name instead of state
(defn refresh-token
  "In case of emergency token refresh, call this function with provider keyword to update
  a specific provider, calling it without arguments tries to update all keys."
  ([provider]
   (let [provider-data (p/provider-reverse-lookup provider @lma/providers)]
     (if @(:refresh_token provider-data)
       (->>
         (lma/get-tokens provider-data)
         (lma/add-to-providers))
       (->>
         (merge provider-data {:code (promise)})
         (lma/init-and-add-provider)))))) 

(defn user-interaction
  "Returns user interaction url if present, nil if not."
  []
  (when-let [auth-url (:auth-url (some #(if-not @(:access_token %) %) (vals @lma/providers)))]
    auth-url))

(defn add-provider
  "Adds provider based on user provided provider-data map and initiates chain
  of function calls to retrieve an oauth token."
  [provider-params]
    (lma/init-and-add-provider (p/create-new-provider provider-params)))

;What if we delete a provider that's in the middle of updating?
(defn delete-provider
  "Remove provider and token data."
  [provider]
  (let [provider-data (p/provider-reverse-lookup provider @lma/providers)
        state (keyword (:state provider-data))]
    (swap! lma/providers dissoc @lma/providers state)))

;Reverser match on provider name instead of state
;Here we either supply our key or don't. If no key, just return (first tokens)
(defn oauth-token
  "Retreive oauth token for use in authentication call"
  [provider]
  (let [provider-data (p/provider-reverse-lookup provider @lma/providers)]
    @(:access_token provider-data)))
