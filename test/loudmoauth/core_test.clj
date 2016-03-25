(ns loudmoauth.core-test
  (:require [clojure.test :refer :all]
            [loudmoauth.core :refer :all]
            [loudmoauth.test-fixtures :as tf]
            [loudmoauth.util :as lmutil]
            [loudmoauth.authflow :as lma]
            [clojure.core.async :as a]))

(use-fixtures :each tf/reset)

(deftest test-parse-code 
  (testing "Test parsing of :code in incoming request."
    (tf/reset-channels)
    (parse-code tf/test-code-http-response)
    (is (= (:code tf/final-state-map) (a/<!! lma/code-chan)))))
 
(deftest test-user-interaction
  (testing "Pull the url used for interaction from channel and publish on end point where hopefully browser is waiting. In the first test we have something on the channel, in the second one the channel is empty."
    (tf/reset-channels)
    (a/go (a/>! lma/interaction-chan (:auth-url tf/final-state-map)))
    (Thread/sleep 2000)
    (is (= (:auth-url tf/final-state-map) (user-interaction))) 
    (is (= nil (user-interaction)))))
 
(deftest test-init
  (testing "Test init function setting parameters and retrieving code and tokens."
    (reset! lma/app-state tf/several-providers-middle-state-map)
    (tf/reset-channels)
    (a/go (a/>! lma/code-chan (:code tf/final-state-map)))
    (with-redefs [clj-http.client/get (constantly (:token-response tf/final-state-map)) clj-http.client/post  (constantly (:token-response tf/final-state-map))]
      (is (= tf/several-providers-final-state-map (update-in (init) [:example] dissoc :token-refresher))))))

(deftest test-set-oauth-params
  (testing "Test setting oauth-params."
    (with-redefs [lmutil/uuid (fn [] "34fFs29kd09")]
      (is (= (update-in tf/several-providers-middle-state-map [:example] dissoc :code) (set-oauth-params tf/start-state-map))))))

(deftest test-oauth-token
  (testing "Retrieve oauth-token from state-map."
    (reset! lma/app-state tf/several-providers-final-state-map)
    (is (= (:access_token tf/final-state-map) (oauth-token :example)))))

