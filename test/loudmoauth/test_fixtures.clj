(ns loudmoauth.test-fixtures
  (:require [clojure.core.async :as a]
            [loudmoauth.authflow :as lma]))





(def test-query-param-string "client_id=5fe01282e44241328a84e7c5cc169165&response_type=code&redirect_uri=https%3A%2F%2Fwww.example.com%2Fcallback&scope=user-read-private+user-read-email&state=34fFs29kd09") 

(def test-custom-param-query-param-string  "client_id=5fe01282e44241328a84e7c5cc169165&response_type=code&redirect_uri=https%3A%2F%2Fwww.example.com%2Fcallback&scope=user-read-private+user-read-email&state=34fFs29kd09&show_dialog=true") 

(def test-encoded-string "SSdtIGdsYWQgSSB3b3JlIHRoZXNlIHBhbnRzLg==")

(def test-form-params-auth {:grant_type "authorization_code"
                       :code "abcdefghijklmn123456789"
                       :redirect_uri "https://www.example.com/callback"})

(def test-form-params-refresh {:grant_type "refresh_token"
                       :refresh_token "sdscgrrf343" })


(def test-parsed-body {:access_token "a12dkdirnc" :refresh_token "sdscgrrf343" :expires_in 1245})

(def test-response-body-string "{\"access_token\":\"a12dkdirnc\",\"refresh_token\":\"sdscgrrf343\",\"expires_in\":1245}")

(def test-enc-auth-string "Basic NWZlMDEyODJlNDQyNDEzMjhhODRlN2M1Y2MxNjkxNjU6MTIzNDU2Nzg5c2VjcmV0")

(def test-headers {:Authorization test-enc-auth-string})

(def test-query-data-auth {:form-params test-form-params-auth :headers test-headers})

(def test-query-data-refresh {:form-params test-form-params-refresh :headers test-headers})

(def test-code-http-response {:status 200 :headers {} :body {} :request-time 0 :trace-redirects ["https://www.example.com/api/token"] :orig-content-encoding nil :params {:state "34fFs29kd09" :code "abcdefghijklmn123456789"}})


(def test-code-http-response {:status 200 :headers {} :body {} :request-time 0 :trace-redirects ["https://www.example.com/api/token"] :orig-content-encoding nil :params {:state "34fFs29kd09" :code "abcdefghijklmn123456789"}})

(def start-state-map
  {:base-url "https://www.example.com"
   :client-id "5fe01282e44241328a84e7c5cc169165"
   :redirect-uri "https://www.example.com/callback"
   :scope "user-read-private user-read-email"
   :custom-query-params {:show-dialog "true"}
   :client-secret "123456789secret"})

(def middle-state-map 
  {:base-url "https://www.example.com"
   :client-id "5fe01282e44241328a84e7c5cc169165"
   :response-type "code"
   :redirect-uri "https://www.example.com/callback"
   :scope "user-read-private user-read-email"
   :state "34fFs29kd09"
   :custom-query-params {:show-dialog "true"}
   :client-secret "123456789secret"
   :encoded-auth-string test-enc-auth-string
   :code "abcdefghijklmn123456789"})

(def final-state-map
  {:base-url "https://www.example.com"
   :client-id "5fe01282e44241328a84e7c5cc169165"
   :response-type "code"
   :redirect-uri "https://www.example.com/callback"
   :scope "user-read-private user-read-email"
   :state "34fFs29kd09"
   :custom-query-params {:show-dialog "true"}
   :client-secret "123456789secret"
   :encoded-auth-string test-enc-auth-string 
   :code "abcdefghijklmn123456789"
   :token-response {:status 200 :headers {} :body test-response-body-string :request-time 0 :trace-redirects ["https://www.example.com/api/token"] :orig-content-encoding nil} 
   :access_token "a12dkdirnc"
   :refresh_token "sdscgrrf343"
   :expires_in 1245 
   :token-url "https://www.example.com/api/token" 
   :auth-url (str "https://www.example.com/authorize/?" test-custom-param-query-param-string)}) 

(defn reset
  "Reset the state a of our app before calling test f."
  [f]
  (reset! lma/app-state {})
  (f))

 
(defn drain!
  [ch]
  (a/go-loop []
           (when (a/poll! ch)
             (recur))))

(defn reset-channels
  []
  "Reset our interaction and code channels to be able to start fresh."
  (drain! lma/code-chan)
  (drain! lma/interaction-chan))


