(ns loudmoauth.authflow-test
  (:require [clojure.test :refer :all]
            [loudmoauth.authflow :refer :all]
            [loudmoauth.util :as lmu]
            [loudmoauth.test-fixtures :as tf]
            [clojure.core.async :as a]))

(use-fixtures :each tf/reset)

;(deftest test-match-code-to-provider)

;(deftest test-fetch-code!)

(deftest test-create-form-params
  (testing "Creation of query parameter map to include in http body."
    (is (= tf/test-form-params-refresh (create-form-params tf/final-state-map)))
    (is (= tf/test-form-params-auth (create-form-params tf/middle-state-map)))))  

(deftest test-add-tokens-to-provider-data
  (testing "Add tokens to provider data."
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

;(deftest test-init-and-add-provider)
