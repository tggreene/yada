;; Copyright © 2014-2017, JUXT LTD.

(ns yada.security-test
  (:require
   [clojure.test :refer :all :exclude [deftest]]
   [schema.test :refer [deftest]]
   [yada.test :refer [response-for]]
   [yada.test-util :refer [with-level]])
  (:import (ch.qos.logback.classic Level)))

(deftest response-header-test
  ;; This test validates that the security module sends the correct headers
  ;; based on the protocol used (http/https). This implies we properly detect
  ;; the protocol, which can be a bit tricky when we're behind a proxy.


  (def echo-routes {:methods {:get {:produces #{"application/edn;q=0.9"}
                                    :response #(:request %)}}})

  (testing "scheme defaults to http"
    (let [response (response-for echo-routes
                                 :get "/" {})
          internal-request (-> response :body read-string)]

      (is (= :http (:scheme internal-request)))))


  ;; Which headers we expect to be set, based on the protocol used
  (def headers {:http ["x-frame-options"
                       "x-xss-protection"
                       "x-content-type-options"]
                :https ["strict-transport-security"
                        "content-security-policy"
                        "x-frame-options"
                        "x-xss-protection"
                        "x-content-type-options"]})


  ;; Different combinations of headers that can be set by proxy servers
  (def header-possibilities [["x-forwarded-proto" "https" :https]
                             ["x-forwarded-proto" "http" :http]
                             ["forwarded-proto" "https" :https]
                             ["forwarded-proto" "http" :http]
                             ["front-end-https" "on" :https]
                             ["front-end-https" "off" :http]])

  (testing "scheme is properly detected from valid header combinations"
    (doseq [[key value expected] header-possibilities]
      (let [response (response-for echo-routes
                                   :get "/" {:headers {key value}})
            internal-request (-> response :body read-string)
            expected-headers (expected headers)]

        (is (= expected (:scheme internal-request)))

        (doseq [header expected-headers]
          (is (contains? (:headers response) header)))))))
