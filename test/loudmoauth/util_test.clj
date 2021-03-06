(ns loudmoauth.util-test
  (:require [clojure.test :refer :all]
            [loudmoauth.util :refer :all]
            [loudmoauth.test-fixtures :as tf]))

(use-fixtures :each tf/reset)

(deftest test-change-keys
  (testing "Change hyphen to underscore for keys in map."
    (is (= {:test_key_1 "1" :test_key_2 "2"}) (change-keys {:test-key-1 "1" :test_key_2 "2"}))))

(deftest test-string-to-base64-string
  (testing "Conversion from normal string to base64 encoded string."
    (is (= tf/test-encoded-string (string-to-base64-string "I'm glad I wore these pants.")))))

(deftest test-parse-json-from-response-body
  (testing "Parse json in http response body"
    (is (= tf/test-parsed-body (parse-json-from-response-body tf/test-response-body-string)))))


