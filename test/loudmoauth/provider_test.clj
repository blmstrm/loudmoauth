(ns loudmoauth.provider-test
  (:require [clojure.test :refer :all]
            [loudmoauth.provider :refer :all]
            [loudmoauth.test-fixtures :as tf]
            [loudmoauth.util :as lmutil]))

(use-fixtures :each tf/reset)

(deftest test-provider-reverse-lookup
  (testing "Performing a reverse lookup of provider data."
    (is (= tf/final-provider-data (provider-reverse-lookup :example tf/final-several-providers-data)))))

(deftest test-query-param-string
  (testing "Create a query param string from given query parameter map."
    (is (= tf/test-custom-param-query-param-string (query-param-string tf/provider-data)))))

(deftest test-auth-url
  (testing "Create auth url from provider data."
    (is (= tf/auth-url (auth-url tf/provider-data)))))

(deftest test-token-url
  "Create token-url form provider data."
  (is (= tf/token-url (token-url tf/provider-data))))

(deftest test-build-provider
  "Build a new provider from a provider data map"
  (with-redefs [lmutil/uuid tf/test-uuid]
 (is (= tf/built-provider (dissoc (build-provider tf/provider-data)  :code :expires_in :refresh_token :access_token)))))

(deftest test-create-new-provider
  "Create a new provider from user input data merged with internally created data. Validate data types."
  (is (create-new-provider tf/new-provider-data-from-user)))
