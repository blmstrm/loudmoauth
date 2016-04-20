(ns loudmoauth.authflow-test
  (:require [clojure.test :refer :all]
            [loudmoauth.authflow :refer :all]
            [loudmoauth.util :as lmu]
            [loudmoauth.test-fixtures :as tf]
            [clojure.core.async :as a]))

(use-fixtures :each tf/reset)

(deftest test-generate-query-param-string
  (testing "Testing generation of query param string with and without :other key"
    (is (= (query-param-string tf/final-state-map) tf/test-custom-param-query-param-string))
    (is (= (query-param-string (dissoc tf/final-state-map :custom-query-params)) tf/test-query-param-string)))) 

(deftest test-add-response-type
  (testing "Testing addition of :response_type key to state map.")
  (is (= (add-response-type "code" (dissoc tf/final-state-map :response-type)) tf/final-state-map)))

(deftest test-add-state
  (testing "Testing addition of :response_type key to state map.")
  (with-redefs [lmu/uuid (fn [] "34fFs29kd09")]
    (is (= (add-state (dissoc tf/final-state-map :state)) tf/final-state-map))))

(deftest test-create-form-params
  (testing "Creation of query parameter map to include in http body."
    (is (= tf/test-form-params-refresh (create-form-params tf/final-state-map)))
    (is (= tf/test-form-params-auth (create-form-params tf/middle-state-map)))))  

(deftest test-add-tokens-to-state-map
  (testing "Add tokens to state map."
    (is (= tf/final-state-map (add-tokens-to-state-map (dissoc tf/final-state-map [:access_token :refresh_token :expires_in]) tf/test-parsed-body)))))

(deftest test-parse-tokens
  (testing "Parse response body from http response and parse tokens from response body add add to state map."
    (is (= tf/final-state-map (parse-tokens (dissoc tf/final-state-map [:access_token :refresh_token :expires_in]))))))

(deftest test-create-query-data 
  (testing "Create query data to use in http post request for tokens."
    (is (= tf/test-query-data-auth (create-query-data tf/middle-state-map)))
    (is (= tf/test-query-data-refresh (create-query-data tf/final-state-map)))))

(deftest test-get-tokens
  (testing "Test retrieving tokens through http requests."
       (with-redefs [clj-http.client/post (constantly (:token-response tf/final-state-map))]
      (is (= tf/final-state-map (dissoc  (get-tokens (dissoc tf/final-state-map [:access_token :refresh_token :expires_in])) :token-refresher))))))

(deftest test-token-url
  (testing "Create token-url."
    (is (= (:token-url tf/final-state-map) (token-url (dissoc tf/final-state-map :token-url))))))

(deftest test-build-token-url
  (testing "Add token-url to state map."
    (is (= tf/final-state-map (build-token-url (dissoc tf/final-state-map [:token-url]))))))

(deftest test-auth-url
  (testing "Create auth-url."
    (is (= (:auth-url tf/final-state-map) (auth-url (dissoc tf/final-state-map [:auth-url]))))))

(deftest test-build-auth-url
  (testing "Add token-url to state map."
    (is (= tf/final-state-map (build-token-url (dissoc tf/final-state-map [:token-url]))))))

(deftest test-request-access-and-refresh-tokens
  (testing "Build url and retrieve tokens."
       ( with-redefs [clj-http.client/post (constantly (:token-response tf/final-state-map))]
     (is (= tf/final-state-map (dissoc (request-access-and-refresh-tokens (dissoc tf/final-state-map [:access_token :refresh_token :expires_in])) :token-refresher))))))

  (deftest test-request-access-to-data
    (testing "Build auth url and fetch code."
    (tf/reset-channels)
        (with-redefs [clj-http.client/get (constantly (:token-response tf/final-state-map))]
       (is (= tf/final-state-map) (request-access-to-data (dissoc tf/final-state-map :code)))))) 

;TODO - test-init-all
;TODO - test-init-one 
;TODO - test-init-provider 
 
(deftest test-init-provider
  (testing "Init a provider given the data."
    (reset! app-state tf/several-providers-middle-state-map)
    (tf/reset-channels)
    (with-redefs [clj-http.client/get (constantly (:token-response tf/final-state-map)) clj-http.client/post  (constantly (:token-response tf/final-state-map))]
      (is (= tf/final-state-map (dissoc (init-provider tf/middle-state-map) :token-refresher))))))

;TODO - Also work out this test.
(deftest test-init-one
  (testing "Init a provider given the provider keyword."
    (reset! app-state tf/several-providers-middle-state-map)
    (tf/reset-channels)
    (with-redefs [clj-http.client/get (constantly (:token-response tf/final-state-map)) clj-http.client/post  (constantly (:token-response tf/final-state-map))]
      (is (= tf/several-providers-final-state-map (update-in  (init-one :example) [:example] dissoc  :token-refresher))))))

(deftest test-init-all
  (testing "Init all provider given the provider keyword."
    (reset! app-state tf/several-providers-middle-state-map)
    (tf/reset-channels)
    (with-redefs [clj-http.client/get (constantly (:token-response tf/final-state-map)) clj-http.client/post  (constantly (:token-response tf/final-state-map))]
      (is (= tf/several-providers-final-state-map (update-in (init-all) [:example] dissoc :token-refresher))))))
