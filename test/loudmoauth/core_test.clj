(ns loudmoauth.core-test
  (:require [clojure.test :refer :all]
            [loudmoauth.core :refer :all]
            [loudmoauth.test-fixtures :as tf]
            [loudmoauth.util :as lmutil]
            [loudmoauth.authflow :as lma]
            [clojure.core.async :as a]))

;TODO - test-parse-params
;TODO - test-init
;TODO - test-set-oauth-params
(use-fixtures :each tf/reset)

(deftest test-parse-params
  (testing "Test parsing of :code and :state in incoming request params."
    (parse-params tf/test-code-http-response)
    (is (= (:code tf/final-state-map) (:code (lmutil/provider-reverse-lookup :example @lma/app-state))))))


(deftest test-user-interaction
  (testing "Pull the url used for interaction from channel and publish on end point where hopefully browser is waiting. In the first test we have something on the channel, in the second one the channel is empty."
    (tf/reset-channels)
    (a/go (a/>! lma/interaction-chan (:auth-url tf/final-state-map)))
    (Thread/sleep 2000)
    (is (= (:auth-url tf/final-state-map) (user-interaction))) 
    (is (= nil (user-interaction)))))

;(deftest test-add-provider)

;(deftest delete-provider)

(deftest test-oauth-token
  (testing "Retrieve oauth-token from state-map."
    (reset! lma/app-state tf/several-providers-final-state-map)
    (is (= (:access_token tf/final-state-map) (oauth-token :example)))))

