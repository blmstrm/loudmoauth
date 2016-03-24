(ns loudmoauth.core
  (:require [clojure.core.async :as a]
            [loudmoauth.authflow :as lma]))

;We need to identify what provider we are coming from here? 
;Together with the code do we get our state? That's our key so that's good.
;We do get our state here. If state isn't present, what else can we use?
;But how do we map a key to another key provided by our users. We need a map where we map user defined keys to our state ids.
(defn parse-code
  "Parse code from http-response and deliver promise."
  [response]
  (a/go
    (->>
      response 
      :params
      :code 
      (a/>! lma/code-chan))))

;Here we pass the name of our provider, :spotify or "spotify" or whatever
(defn refresh-token
  "In case of emergency token refresh, we supply this function."
  ([] (lma/get-tokens (first @lma/app-state)))
  ([provider] (lma/get-tokens (provider @lma/app-state))))

;Should we just init everything at once.
;If not args passed map init functions over every key in app. If provider name specified init only that provider.
(defn init
  "Init oauth token request cycle."
  ([] (lma/init-all))
  ([provider] (lma/init-one provider)))

;This is the handlers function to retrieve different kinds of dialog pages, nescessary for identification.
(defn user-interaction
  "Prompt for user interaction."
  []
  (if-let [interaction-url (a/poll! lma/interaction-chan)]
    interaction-url
    "No user interaction nescessary."))

;We can totally get away with only rewriting this namespace, just make each function take a keyword as argument, no argument, give us first in list.
;What do we do if we don't have provider name as key:value?"
(defn set-oauth-params
  "Set oauth-parameters for use in call to get token"
  [params]
  (let [old-app-state @lma/app-state
        provider-name (:provider params)
        ]
    (->>
      (if-not (:response-type params)
        (lma/add-response-type "code" params)
        params)
      (lma/add-state) 
      (merge old-app-state)
      (lma/add-encoded-auth-string)
      (swap! lma/app-state assoc provider-name))))

;Here we either supply our key or don't. If no key, just return (first tokens)
(defn oauth-token
  "Retreive oauth token for use in authentication call"
  ([provider] (:access_token (provider @lma/app-state))))
