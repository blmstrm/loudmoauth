(ns loudmoauth.test-fixtures
  (:require [clojure.core.async :as a]
            [loudmoauth.authflow :as lma]))

(def test-state-value "34fFs29kd09")

(def test-state-value-keyword (keyword test-state-value))

(def test-query-param-string "client_id=5fe01282e44241328a84e7c5cc169165&response_type=code&redirect_uri=https%3A%2F%2Fwww.example.com%2Fcallback&scope=user-read-private+user-read-email&state=34fFs29kd09") 

(def test-custom-param-query-param-string  "client_id=5fe01282e44241328a84e7c5cc169165&response_type=code&redirect_uri=https%3A%2F%2Fwww.example.com%2Fcallback&scope=user-read-private+user-read-email&state=34fFs29kd09&show_dialog=true") 

(def test-encoded-string "SSdtIGdsYWQgSSB3b3JlIHRoZXNlIHBhbnRzLg==")

(def test-form-params-auth {:grant_type "authorization_code"
                       :code "abcdefghijklmn123456789"
                       :redirect_uri "https://www.example.com/callback" :client_id "5fe01282e44241328a84e7c5cc169165"
                       :client_secret "123456789secret"})

(def test-form-params-refresh {:grant_type "refresh_token"
                       :refresh_token "sdscgrrf343"
                       :client_id "5fe01282e44241328a84e7c5cc169165"
                       :client_secret "123456789secret"})

(def test-parsed-body {:access_token "a12dkdirnc" :refresh_token "sdscgrrf343" :expires_in 1245})

(def test-response-body-string "{\"access_token\":\"a12dkdirnc\",\"refresh_token\":\"sdscgrrf343\",\"expires_in\":1245}")

(def test-query-data-auth {:form-params test-form-params-auth})
(def test-query-data-refresh {:form-params test-form-params-refresh})

(def test-code-http-response {:status 200 :headers {} :body {} :request-time 0 :trace-redirects ["https://www.example.com/api/token"] :orig-content-encoding nil :params {:state test-state-value :code "abcdefghijklmn123456789"}})

(def test-state-code-params (:params test-code-http-response))

(def auth-url (str "https://www.example.com/authorize/?" test-custom-param-query-param-string))

(def token-url "https://www.example.com/api/token") 

(def code-params {:state test-state-value :code "abcdefghijklmn123456789"})

(def new-provider-data
  {:base-url "https://www.example.com"
   :auth-endpoint "/authorize"
   :token-endpoint "/api/token"
   :client-id "5fe01282e44241328a84e7c5cc169165"
   :redirect-uri "https://www.example.com/callback"
   :scope "user-read-private user-read-email"
   :custom-query-params {:show-dialog "true"}
   :client-secret "123456789secret"
   :provider :example})

(def provider-data
  {:base-url "https://www.example.com"
   :auth-endpoint "/authorize"
   :token-endpoint "/api/token"
   :client-id "5fe01282e44241328a84e7c5cc169165"
   :response-type "code"
   :redirect-uri "https://www.example.com/callback"
   :scope "user-read-private user-read-email"
   :state test-state-value
   :custom-query-params {:show-dialog "true"}
   :client-secret "123456789secret"
   :provider :example
   :code (promise)
   :auth-url (str "https://www.example.com/authorize/?" test-custom-param-query-param-string)
   :token-url "https://www.example.com/api/token" 
   })

(def built-provider
  {:state test-state-value
   :auth-url auth-url 
   :response-type  "code"
   :token-url token-url})

(def final-provider-data
  {:base-url "https://www.example.com"
   :auth-endpoint "/authorize"
   :token-endpoint "/api/token"
   :client-id "5fe01282e44241328a84e7c5cc169165"
   :response-type "code"
   :redirect-uri "https://www.example.com/callback"
   :scope "user-read-private user-read-email"
   :state test-state-value
   :custom-query-params {:show-dialog "true"}
   :client-secret "123456789secret"
   :code (promise)
   :token-response {:status 200 :headers {} :body test-response-body-string :request-time 0 :trace-redirects ["https://www.example.com/api/token"] :orig-content-encoding nil} 
   :access_token "a12dkdirnc"
   :refresh_token "sdscgrrf343"
   :expires_in 1245 
   :auth-url (str "https://www.example.com/authorize/?" test-custom-param-query-param-string)
   :provider :example}) 

(def several-providers-data
  {test-state-value-keyword provider-data})
 
(def final-several-providers-data
  {test-state-value-keyword final-provider-data})

(defn reset
  "Reset the state a of our app before calling test f."
  [f]
  (deliver (:code final-provider-data) "abcdefghijklmn123456789")
  (deliver (:code provider-data) "abcdefghijklmn123456789")
  (f))

(defn drain!
  [ch]
  (a/go-loop []
           (when (a/poll! ch)
             (recur))))

(defn reset-channels
  []
  "Reset our interaction and code channels to be able to start fresh."
  (drain! lma/interaction-chan))

(defn test-uuid
  "Always return the same uuid for testin purposes."
  []
  test-state-value)

