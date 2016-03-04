(ns loudmoauth.core-test
  (:require [clojure.test :refer :all]
            [loudmoauth.core :refer :all]
            [clojure.core.async :as a]
            [clj-http.fake :refer :all]))


(def test-query-param-string "client_id=5fe01282e44241328a84e7c5cc169165&response_type=code&redirect_uri=https%3A%2F%2Fwww.example.com%2Fcallback&scope=user-read-private+user-read-email&state=34fFs29kd09") 

(def test-custom-param-query-param-string  "client_id=5fe01282e44241328a84e7c5cc169165&response_type=code&redirect_uri=https%3A%2F%2Fwww.example.com%2Fcallback&scope=user-read-private+user-read-email&state=34fFs29kd09&show_dialog=true") 

(def test-encoded-string "SSdtIGdsYWQgSSB3b3JlIHRoZXNlIHBhbnRzLg==")

(def test-body-params {:grant_type "authorization_code"
                       :code "abcdefghijklmn123456789"
                       :redirect_uri "https://www.example.com/callback"})

(def test-parsed-body {:access_token "a12dkdirnc" :refresh_token "sdscgrrf343" :expires_in 1245})

(def test-response-body-string "{\"access_token\":\"a12dkdirnc\",\"refresh_token\":\"sdscgrrf343\",\"expires_in\":1245}")

(def test-enc-auth-string "Basic NWZlMDEyODJlNDQyNDEzMjhhODRlN2M1Y2MxNjkxNjU6MTIzNDU2Nzg5c2VjcmV0")

(def test-headers {:Authorization test-enc-auth-string})

(def test-query-data {:body test-body-params :headers test-headers})

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
   })

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
   :auth-url (str "https://www.example.com/authorize/?" test-custom-param-query-param-string) 
   }) 

(defn drain!
  [ch]
  (a/go-loop []
           (when (a/poll! ch)
             (recur))))

(defn reset-channels
  []
  "Reset our interaction and code channels to be able to start fresh."
  (drain! code-chan)
  (drain! interaction-chan))

(defn reset
  "Reset the state of our app."
  [f]
  (reset! app-state {})
  (f))

(use-fixtures :each reset)

(deftest test-generate-query-param-string
  (testing "Testing generation of query param string with and without :other key"
    (is (= (query-param-string final-state-map) test-custom-param-query-param-string))
    (is (= (query-param-string (dissoc final-state-map :custom-query-params)) test-query-param-string)))) 

(deftest test-add-response-type
  (testing "Testing addition of :response_type key to state map.")
  (is (= (add-response-type "code" (dissoc final-state-map :response-type)) final-state-map)))

(deftest test-add-state
  (testing "Testing addition of :response_type key to state map.")
  (with-redefs [uuid (fn [] "34fFs29kd09")]
    (is (= (add-state (dissoc final-state-map :state)) final-state-map))))

(deftest test-parse-code 
  (testing "Test parsing of :code in incoming request."
    (parse-code test-code-http-response)
    (is (= (:code final-state-map) (a/<!! code-chan)))))

(deftest test-fetch-code
  (testing "Test issuing http get for auth-code from oauth provider."
   (reset-channels)
   (a/go (a/>! code-chan (:code final-state-map)))
   (with-redefs [clj-http.client/get (constantly (:token-response final-state-map))]
      (is (= final-state-map (fetch-code final-state-map))))))

(deftest test-user-interaction
  (testing "Pull response from http-requests for authorization and deliver to browser.
           In the first test we have something on the channel, in the second one the channel is empty."
    (reset-channels)
    (a/go (a/>! interaction-chan (:token-response final-state-map)))
    (Thread/sleep 1000)
    (is (= (:token-response final-state-map) (user-interaction {:status 200}))) 
    (is (= "No user interaction nescessary." (user-interaction {:status 200})))))

(deftest test-string-to-base64-string
  (testing "Conversion from normal string to base64 encoded string."
    (is (= (string-to-base64-string "I'm glad I wore these pants.")))))

(deftest test-encoded-auth-string
  (testing "Base64 encode auth-string."
    (is (= (:encoded-auth-string final-state-map) (encoded-auth-string (dissoc  final-state-map :encoded-auth-string))))))

(deftest test-add-encoded-auth-string
  (testing "Adding encoded auth string to state map"
    (is (= (add-encoded-auth-string (dissoc final-state-map :encoded-auth-string)) final-state-map))))

(deftest test-create-body-params
  (testing "Creation of query parameter map to include in http body."
    (is (= test-body-params (create-body-params final-state-map)))))

(deftest test-add-tokens-to-state-map
  (testing "Add tokens to state map."
    (is (= final-state-map (add-tokens-to-state-map (dissoc final-state-map [:access_token :refresh_token :expires_in]) test-parsed-body)))))

(deftest test-parse-json-from-response-body
  (testing "Parse json in http response body")
  (is (= test-parsed-body (parse-json-from-response-body test-response-body-string))))

(deftest test-parse-tokens
  (testing "Parse response body from http response and parse tokens from response body add add to state map."
    (is (= final-state-map (parse-tokens (dissoc final-state-map [:access_token :refresh_token :expires_in]))))))

(deftest test-create-headers
  (testing "Create http headers to use in post request for tokens."
    (is (= test-headers (create-headers final-state-map)))))

(deftest test-create-query-data 
  (testing "Create query data to use in http post request for tokens."
    (is (= test-query-data (create-query-data final-state-map)))))

(deftest test-get-tokens
  (testing "Test retrieving tokens through http requests."
       (with-redefs [clj-http.client/post (constantly (:token-response final-state-map))]
      (is (= final-state-map (update-in (get-tokens (dissoc final-state-map [:access_token :refresh_token :expires_in])) [:token-response :request-time] (fn [x] 0)))))))

(deftest test-token-url
  (testing "Create token-url."
    (is (= (:token-url final-state-map) (token-url (dissoc final-state-map :token-url))))))

(deftest test-build-token-url
  (testing "Add token-url to state map."
    (is (= final-state-map (build-token-url (dissoc final-state-map [:token-url]))))))

(deftest test-auth-url
  (testing "Create auth-url."
    (is (= (:auth-url final-state-map) (auth-url (dissoc final-state-map [:auth-url]))))))

(deftest test-build-auth-url
  (testing "Add token-url to state map."
    (is (= final-state-map (build-token-url (dissoc final-state-map [:token-url]))))))

(deftest test-request-access-and-refresh-tokens
  (testing "Build url and retrieve tokens."
       ( with-redefs [clj-http.client/post (constantly (:token-response final-state-map))]
     (is (= final-state-map (update-in (request-access-and-refresh-tokens (dissoc final-state-map [:access_token :refresh_token :expires_in])) [:token-response :request-time] (fn [x] 0)))))))

  (deftest test-request-access-to-data
    (testing "Build auth url and fetch code."
    (reset-channels)
    (a/go (a/>! code-chan (:code final-state-map)))
        (with-redefs [clj-http.client/get (constantly (:token-response final-state-map))]
       (is (= final-state-map) (update-in  (request-access-to-data (dissoc final-state-map :code)) [:token-response :request-time] (fn [x] 0)))))) 

(deftest test-init
  (testing "Test init function setting parameters and retrieving code and tokens."
    (reset! app-state middle-state-map)
    (reset-channels)
    (a/go (a/>! code-chan (:code final-state-map)))
    (with-redefs [clj-http.client/get (constantly (:token-response final-state-map)) clj-http.client/post  (constantly (:token-response final-state-map))]
      (is (= final-state-map (update-in (init) [:token-response :request-time] (fn [x] 0)))))))

(deftest test-set-oauth-params
  (testing "Test setting oauth-params."
    (reset! app-state {})
    (with-redefs [uuid (fn [] "34fFs29kd09")]
      (is (= middle-state-map (set-oauth-params start-state-map))))))

(deftest test-oauth-token
  (testing "Retrieve oauth-token from state-map."
    (reset! app-state final-state-map)
    (is (= (:access_token final-state-map) (oauth-token)))))

