(ns loudmoauth.core
  (:require [clojure.core.async :as a]
            [loudmoauth.authflow :as lma]))

;We need to identify what provider we are coming from here? 
;Together with the code do we get our state? That's our key so that's good.
;We do get our state here. If state isn't present, what else can we use?
;But how do we map a key to another key provided by our users. We need a map where we map user defined keys to our state ids.
;Don't use a channel. Just do your mapping and be done.
(defn parse-params
  "Parse parameters from http-response and put on channel."
  [response]
  (a/go
    (->>
      response 
      :params
      (a/>! lma/params-chan))))

(defn refresh-token
  "In case of emergency token refresh, call this function with provider keyword to update
  a specific provider, calling it without arguments tries to update all keys."
  ([] (map lma/get-tokens @lma/app-state))
  ([provider] (lma/get-tokens (provider @lma/app-state))))

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
  (let [old-app-state @lma/app-state
        provider (:provider params)
        ]
    (->>
      (if-not (:response-type params)
        (lma/add-response-type "code" params)
        params)
      (lma/add-state) 
      (swap! lma/app-state assoc provider))))

;Here we either supply our key or don't. If no key, just return (first tokens)
(defn oauth-token
  "Retreive oauth token for use in authentication call"
  ([provider] (:access_token (provider @lma/app-state))))
