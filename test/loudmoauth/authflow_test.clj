(ns loudmoauth.authflow-test
  (:require [clojure.test :refer :all]
            [loudmoauth.authflow :refer :all]
            [loudmoauth.util :as lmu]
            [loudmoauth.test-fixtures :as tf]
            [clojure.core.async :as a]))

;(deftest test-match-code-to-provider)
(deftest test-match-code-to-provider
  (testing "Match a given state and code to a certain provider."
    (with-redefs [providers (atom tf/several-providers-final-state-map)]  
      (match-code-to-provider tf/code-params)
      (is (= (:code tf/code-params) @(:code ((keyword tf/test-state-value)  @providers)))))))

(deftest test-fetch-code!
  (testing "Test putting auth url on interaction channel.")
  (fetch-code! (:auth-url tf/final-state-map))
  (is (= (:auth-url tf/final-state-map) (a/<!! interaction-chan))))

;(deftest test-create-form-params
;  (testing "Creation of query parameter map to include in http body."
;    (is (= tf/test-form-params-refresh (create-form-params tf/final-state-map)))
;    (is (= tf/test-form-params-auth (create-form-params tf/middle-state-map)))))  

;(deftest test-add-tokens-to-provider-data)

;(deftest test-parse-tokens!)

;(deftest test-create-query-data)

;(deftest test-get-tokens)

;(deftest test-init-and-add-provider)
