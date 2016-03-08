(ns loudmoauth.core-test
  (:require [clojure.test :refer :all]
            [loudmoauth.core :refer :all]
            [loudmoauth.util-test :as lmutil]
            [clojure.core.async :as a]))

(use-fixtures :each reset)

(deftest test-parse-code 
  (testing "Test parsing of :code in incoming request."
    (parse-code lmutil/test-code-http-response)
    (is (= (:code final-state-map) (a/<!! code-chan)))))
 
(deftest test-user-interaction
  (testing "Pull response from http-requests for authorization and deliver to browser.
           In the first test we have something on the channel, in the second one the channel is empty."
    (reset-channels)
    (a/go (a/>! interaction-chan (:token-response final-state-map)))
    (Thread/sleep 2000)
    (is (= (:token-response final-state-map) (user-interaction {:status 200}))) 
    (is (= "No user interaction nescessary." (user-interaction {:status 200})))))
 
(deftest test-init
  (testing "Test init function setting parameters and retrieving code and tokens."
    (reset! app-state middle-state-map)
    (reset-channels)
    (a/go (a/>! code-chan (:code final-state-map)))
    (with-redefs [clj-http.client/get (constantly (:token-response final-state-map)) clj-http.client/post  (constantly (:token-response final-state-map))]
      (is (= final-state-map (dissoc (init) :token-refresher))))))

(deftest test-set-oauth-params
  (testing "Test setting oauth-params."
    (with-redefs [uuid (fn [] "34fFs29kd09")]
      (is (= (dissoc middle-state-map :code) (set-oauth-params start-state-map))))))

(deftest test-oauth-token
  (testing "Retrieve oauth-token from state-map."
    (reset! app-state final-state-map)
    (is (= (:access_token final-state-map) (oauth-token)))))

