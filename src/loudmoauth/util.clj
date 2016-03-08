(ns loudmoauth.util
 (:require [clojure.data.codec.base64 :as b64]
            [clojure.data.json :as json]))

(defn uuid [] (str (java.util.UUID/randomUUID)))

;TODO - Should omit bringing in the vals here?
(defn change-keys 
  "Change hyphen to underscore in param map keys."
  [params]
  (if-not params
    nil
    (let [ks (keys params)
          vs (vals params)
          new-keys (map #(keyword (str/replace (name %) "-" "_")) ks)]
      (zipmap new-keys vs))))

 (defn string-to-base64-string
  "b64 encode string"
  [original]
  (String. (b64/encode (.getBytes original)) "UTF-8"))

 (defn parse-json-from-response-body
  "Parse json in response body"
  [response-body]
  (json/read-str response-body :key-fn keyword))


 
