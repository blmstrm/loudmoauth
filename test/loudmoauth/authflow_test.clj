(ns loudmoauth.authflow-test
  (:require [clojure.test :refer :all]
            [loudmoauth.authflow :refer :all]
            [loudmoauth.util :as lmu]
            [loudmoauth.test-fixtures :as tf]
            [clojure.core.async :as a]))

(deftest test-match-code-to-provider
  (testing "Match a given state and code to a certain provider."
    (with-redefs [providers (atom tf/final-several-providers-data)]  
      (match-code-to-provider tf/code-params)
      (is (= (:code tf/code-params) @(:code ((keyword tf/test-state-value)  @providers)))))))

(deftest test-fetch-code!
  (testing "Test putting auth url on interaction channel.")
  (fetch-code! (:auth-url tf/final-provider-data))
  (is (= (:auth-url tf/final-provider-data) (a/<!! interaction-chan))))

(deftest test-create-form-params
  (testing "Creation of query parameter map to include in http body."
    (deliver (:code tf/provider-data) "abcdefghijklmn123456789")
    (deliver (:code tf/final-provider-data) "abcdefghijklmn123456789")
    (is (= tf/test-form-params-auth (create-form-params tf/provider-data)))
    (is (= tf/test-form-params-refresh (create-form-params tf/final-provider-data)))))  

(deftest test-add-tokens-to-provider-data
 (testing "Add access token, refresh token and expires in values to current provider."
   (add-tokens-to-provider-data tf/provider-data tf/test-parsed-body)
   (is (= @(:access_token tf/provider-data) @(:access_token tf/final-provider-data)))
   (is  (= @(:refresh_token tf/provider-data) @(:refresh_token tf/final-provider-data)))
   (is (= @(:expires_in tf/provider-data) @(:expires_in tf/final-provider-data)))))

(deftest test-parse-tokens!
 (testing "Parses token information from response body."
   (parse-tokens! tf/provider-data-with-token-response)
   (is (= @(:access_token tf/provider-data-with-token-response) @(:access_token tf/final-provider-data)))
   (is  (= @(:refresh_token tf/provider-data-with-token-response) @(:refresh_token tf/final-provider-data)))
   (is (= @(:expires_in tf/provider-data-with-token-response) @(:expires_in tf/final-provider-data)))))

(deftest test-create-query-data
  (testing "Create query data map from provider data."
    (deliver (:code tf/provider-data) "abcdefghijklmn123456789")
    (deliver (:code tf/final-provider-data) "abcdefghijklmn123456789")
    (is (=  tf/test-query-data-auth (create-query-data tf/provider-data)))
    (is (=  tf/test-query-data-refresh (create-query-data tf/final-provider-data)))))

;(deftest test-get-tokens)
(deftest test-get-tokens
  (testing "Retrieve tokens from authentication server, parse the reply and add the token information to our provider-data,"
    (with-redefs [http-post-for-tokens (fn [provider-data] tf/test-token-response)]
      (get-tokens tf/provider-data)
   (is (= @(:access_token tf/final-provider-data) @(:access_token tf/provider-data)))
   (is  (= @(:refresh_token tf/final-provider-data) @(:refresh_token tf/provider-data)))
   (is (= @(:expires_in tf/final-provider-data) @(:expires_in tf/provider-data))))))

;(deftest test-init-and-add-provider)
