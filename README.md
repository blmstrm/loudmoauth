[![Build Status](https://travis-ci.org/blmstrm/loudmoauth.svg?branch=master)](https://travis-ci.org/blmstrm/loudmoauth)
[![Clojars](https://img.shields.io/clojars/v/loudmoauth.svg)](http://clojars.org/loudmoauth)
[![Coverage Status](https://coveralls.io/repos/github/blmstrm/loudmoauth/badge.svg?branch=master)](https://coveralls.io/github/blmstrm/loudmoauth?branch=master)
[![Dependencies Status](https://jarkeeper.com/blmstrm/loudmoauth/status.svg)](https://jarkeeper.com/blmstrm/loudmoauth)
#Loudmoauth
Loudmoauth is a general ouath2 client library. 

##Quickstart
To use `loudmoauth` with Leiningen or Boot include `[loudmoauth.core "0.1.0"]`.

Require `loudmoauth` in your application:
```Clojure
(ns my-app.core
  (:require [loudmoauth.core :as lmoauth]))
```
Start with creating a map with the specific parameters regarding your oauth2 service provider, here Spotify is used an example. Observer that I've stashed the client id and secret in system variables but that is up to the user:
  ```Clojure
(def spotify-oauth2-params
  {:base-url "https://accounts.spotify.com"
   :auth-endpoint "/authorize"
   :token-endpoint "/api/token"
   :client-id (System/getenv "SPOTIFY_OAUTH2_CLIENT_ID")
   :redirect-uri "http://localhost:3000/oauth2"
   :scope "playlist-read-private user-follow-modify"
   :custom-query-params {:show-dialog "true"}
   :client-secret (System/getenv "SPOTIFY_OAUTH2_CLIENT_SECRET")
   :provider :spotify})
```
Configure your http-request handler to call the function `parse-params` when the url  specified for `:redirect-uri` is called. The function `parse-params` expects a ring http response as input argument:
```Clojure
(defn handler [request]
  (condp = (:uri request)
     "/oauth2" (lm/parse-params request)
      "/interact"  (ringr/redirect (lm/user-interaction))  
    {:status 200
     :body (:uri request)}))
```
Pass the map specified earlier as an argument to the `add-provider` function.
```Clojure
(lmoauth/add-provider spotify-oauth2-params)
```
To retrieve your token call the `oauth-token` function with the keyword for the provider that you specified in your parameter map earlier:
```Clojure
(lmouath/oauth-token :spotify)
```
This should be it. For a more detailed explanation see below. For working examples see the repository [loudmoauth-examples](https://github.com/blmstrm/loudmoauth-examples).

##In-depth documentation
##oauth-params map
`:base-url`
`:auth-endpoint`
`:token-endpoint` 
`:client-id `
`:redirect-uri` 
`:scope`
`:custom-query-params`
`:client-secret`
`:provider`

##When/If the token expires
When a new provider is added to the client

##Removing tokens

##Exception handling

## License
The MIT License (MIT)

	Copyright (c) 2016 Karl Blomström

	Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:

	The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.

	THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
