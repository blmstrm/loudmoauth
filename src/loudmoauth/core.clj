(ns loudmoauth.core
  (:require [clojure.core.async :as a]
            [loudmoauth.authflow :as lma]
            [loudmoauth.util :as util]))

;Use channel here? Calling a function like this is so nesting.
(defn parse-params
  "Parse parameters from http-response and put on channel."
  [response]
  (a/go
    (->>
      response 
      :params
      (lma/match-code-to-provider))))

;Reverser match on provider name instead of state
(defn refresh-token
  "In case of emergency token refresh, call this function with provider keyword to update
  a specific provider, calling it without arguments tries to update all keys."
  ([] (map lma/get-tokens @lma/app-state))
  ([provider]
   (let [provider-data util/provider-reverse-lookup provider @lma/app-state]
     (lma/get-tokens provider-data))))

(defn init
  "Initiate oauth token request cycle. If called with no arguments we init all providers.
  To init one specific provider supply the provider keyword."
  ([] (lma/init-all))
  ([provider] (lma/init-one provider)))

(defn user-interaction
  "Returns user interaction url if present, nil if not."
  []
  (if-let [interaction-url (a/poll! lma/interaction-chan)]
    interaction-url
    nil))

;We can totally get away with only rewriting this namespace, just make each function take a keyword as argument, no argument, give us first in list.
;What do we do if we don't have provider name as key:value?"
(defn set-oauth-params
  "Set oauth-parameters for use in call to get token"
  [params]
  (let [old-app-state @lma/app-state ]
    (->>
      (if-not (:response-type params)
        (lma/add-response-type "code" params)
        params)
      (lma/add-state) 
      #(swap! lma/app-state assoc (keyword (:state %)) %))))

;Reverser match on provider name instead of state
;Here we either supply our key or don't. If no key, just return (first tokens)
(defn oauth-token
  "Retreive oauth token for use in authentication call"
  ([provider]
   (let [provider-data (util/provider-reverse-lookup provider @lma/app-state)]
     (:access_token provider-data))))
