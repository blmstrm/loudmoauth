(ns loudmoauth.core
  (:require [clojure.core.async :as a]
            [loudmoauth.authflow :as lma]))

;TODO - Deal with errors and exceptions
;TODO - Deal with missing :redirect-uri key, if that is possible?
;TODO - Go over doc strings one more time.
;TODO - How do we support several service-providers at the same time?

;This is the handlers function to call after receiving callback to specified url.
;TODO - Deal with cases where token is nil. If so don't put on channel.
;TODO - What do we return here?
(defn parse-code
  "Parse code from http-response and deliver promise."
  [response]
  (a/go
    (->>
      response 
      :params
      :code 
      (a/>! lma/code-chan))))

(defn refresh-token
  []
  "In case of emergency token refresh, we supply this function."
  (lma/get-tokens @lma/app-state))

(defn init
  "Init ouath token request cycle."
  []
  (let [old-app-state @lma/app-state] 
    (->>
      old-app-state
      (lma/request-access-to-data)
      (lma/request-access-and-refresh-tokens)
      (reset! lma/app-state))))

;This is the handlers function to retrieve different kinds of dialog pages, nescessary for identification.
(defn user-interaction
  "Prompt for user interaction."
  []
  (if-let [interaction-url (a/poll! lma/interaction-chan)]
    interaction-url
    "No user interaction nescessary."))
 
(defn set-oauth-params
  "Set oauth-parameters for use in call to get token"
  [params]
  (let [old-app-state @lma/app-state]
    (->>
      (if-not (:response-type params)
        (lma/add-response-type "code" params)
        params)
      (lma/add-state) 
      (merge old-app-state)
      (lma/add-encoded-auth-string)
      (reset! lma/app-state))))

(defn oauth-token
  "Retreive oauth token for use in authentication call"
  []
  (:access_token @lma/app-state))
