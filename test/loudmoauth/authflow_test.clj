(ns loudmoauth.authflow-test
  (:require [clojure.test :refer :all]
            [loudmoauth.authflow :refer :all]
            [loudmoauth.test-util :as lmutil]
            [clojure.core.async :as a]))

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


(deftest test-fetch-code
  (testing "Test issuing http get for auth-code from oauth provider."
   (reset-channels)
   (a/go (a/>! code-chan (:code final-state-map)))
   (with-redefs [clj-http.client/get (constantly (:token-response final-state-map))]
      (is (= final-state-map (fetch-code final-state-map))))))

(deftest test-encoded-auth-string
  (testing "Base64 encode auth-string."
    (is (= (:encoded-auth-string final-state-map) (encoded-auth-string (dissoc  final-state-map :encoded-auth-string))))))

(deftest test-add-encoded-auth-string
  (testing "Adding encoded auth string to state map"
    (is (= (add-encoded-auth-string (dissoc final-state-map :encoded-auth-string)) final-state-map))))

(deftest test-create-form-params
  (testing "Creation of query parameter map to include in http body."
    (is (= test-form-params-refresh (create-form-params final-state-map)))
    (is (= test-form-params-auth (create-form-params middle-state-map)))))  

(deftest test-add-tokens-to-state-map
  (testing "Add tokens to state map."
    (is (= final-state-map (add-tokens-to-state-map (dissoc final-state-map [:access_token :refresh_token :expires_in]) test-parsed-body)))))

(deftest test-parse-tokens
  (testing "Parse response body from http response and parse tokens from response body add add to state map."
    (is (= final-state-map (parse-tokens (dissoc final-state-map [:access_token :refresh_token :expires_in]))))))

(deftest test-create-headers
  (testing "Create http headers to use in post request for tokens."
    (is (= test-headers (create-headers final-state-map)))))

(deftest test-create-query-data 
  (testing "Create query data to use in http post request for tokens."
    (is (= test-query-data-auth (create-query-data middle-state-map)))
    (is (= test-query-data-refresh (create-query-data final-state-map)))))

(deftest test-get-tokens
  (testing "Test retrieving tokens through http requests."
       (with-redefs [clj-http.client/post (constantly (:token-response final-state-map))]
      (is (= final-state-map (dissoc  (get-tokens (dissoc final-state-map [:access_token :refresh_token :expires_in])) :token-refresher))))))

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
     (is (= final-state-map (dissoc (request-access-and-refresh-tokens (dissoc final-state-map [:access_token :refresh_token :expires_in])) :token-refresher))))))

  (deftest test-request-access-to-data
    (testing "Build auth url and fetch code."
    (reset-channels)
    (a/go (a/>! code-chan (:code final-state-map)))
        (with-redefs [clj-http.client/get (constantly (:token-response final-state-map))]
       (is (= final-state-map) (request-access-to-data (dissoc final-state-map :code)))))) 
 



