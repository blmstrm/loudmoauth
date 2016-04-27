(ns loudmoauth.provider
  (:require [clojure.string :as str]
            [clj-http.client :as client] 
            [schema.core :as s]
            [loudmoauth.util :as util]))

(s/def internal-provider-data
  {:code s/Any
   :token-data (s/atom {(s/required-key :expires_in) (s/either s/Int nil)
                        (s/required-key :refresh_token) (s/either s/Str nil)
                        (s/required-key :access_token) (s/either s/Str nil)})
   :state  s/Str
   :auth-url s/Str
   :respone-type  s/Str
   :token-url  s/Str})

(s/def user-provider-data
  {:client-secret  s/Str
   :base-url s/Str
   :auth-endpoint s/Str
   :token-endpoint s/Str
   :client-id  s/Str
   (s/optional-key :custom-query-params) {s/Keyword s/Str} 
   :provider  s/Keyword
   :redirect-uri  s/Str
   :scope   s/Str}) 

(def query-params [:client-id :response-type :redirect-uri :scope :state])

(defn provider-reverse-lookup
  "Performs a reverse look up on provider value and returns the first result found."
  [provider m]
  (some #(if (= provider (:provider %)) %) (vals m)))

(defn query-param-string 
  "Get query-param string from query parameter map."
  [provider-data]
  (->>
    (:custom-query-params provider-data) (merge (select-keys provider-data query-params))
    (util/change-keys)
    (client/generate-query-string)))

(defn auth-url
  "Build the authorization url."
  [provider-data]
  (str (:base-url provider-data) (:auth-endpoint provider-data) "/?" (query-param-string provider-data)))

(defn token-url
  "Build the url for retreieving tokens."
  [provider-data]
  (str (:base-url provider-data) (:token-endpoint provider-data)))

(defn build-provider
  [provider-data]
  {:code (promise)
   :token-data (atom {:expires_in nil
                      :refresh_token nil
                      :access_token nil})
   :state (util/uuid)
   :auth-url (auth-url provider-data)
   :response-type  "code"
   :token-url (token-url provider-data)})

(defn create-new-provider
  [new-provider-data]
  (let [validated-user-data (s/validate user-provider-data new-provider-data)
        validated-internal-data (s/validate internal-provider-data (build-provider validated-user-data))] 
    (merge validated-user-data validated-internal-data)))


