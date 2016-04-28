(ns loudmoauth.provider-test
  (:require [clojure.test :refer :all]
            [loudmoauth.provider :refer :all]
            [loudmoauth.test-fixtures :as tf]
            [loudmoauth.util :as lmutil]))

(deftest test-provider-reverse-lookup
  (testing "Performing a reverse lookup of provider data."
    (is (= tf/final-state-map (provider-reverse-lookup :example tf/several-providers-final-state-map)))))

;(deftest test-query-param-string)

;(deftest test-auth-url)

;(deftest token-url)

;(deftest build-provider)

;(deftest create-new-provider)

