(ns loudmoauth.util-test
  (:require [clojure.test :refer :all]
            [loudmoauth.util :refer :all]
            [loudmoauth.test-fixtures :as tf]
            [clojure.core.async :as a]))

(deftest test-string-to-base64-string
  (testing "Conversion from normal string to base64 encoded string."
    (is (= tf/test-encoded-string (string-to-base64-string "I'm glad I wore these pants.")))))

(deftest test-parse-json-from-response-body
  (testing "Parse json in http response body"
    (is (= tf/test-parsed-body (parse-json-from-response-body tf/test-response-body-string)))))

(deftest test-provider-reverse-lookup
  (testing "Performing a reverse lookup of provider data."
    (is (= tf/final-state-map (provider-reverse-lookup :example tf/several-providers-final-state-map)))))

