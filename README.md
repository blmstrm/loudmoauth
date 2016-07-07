[![Build Status](https://travis-ci.org/blmstrm/loudmoauth.svg?branch=master)](https://travis-ci.org/blmstrm/loudmoauth)
[![Clojars](https://img.shields.io/clojars/v/loudmoauth.svg)](http://clojars.org/loudmoauth)
[![Coverage Status](https://coveralls.io/repos/github/blmstrm/loudmoauth/badge.svg?branch=master)](https://coveralls.io/github/blmstrm/loudmoauth?branch=master)
[![Dependencies Status](https://jarkeeper.com/blmstrm/loudmoauth/status.svg)](https://jarkeeper.com/blmstrm/loudmoauth)
#Loudmoauth
Loudmoauth is a single user multi provider oauth2 client library. It's been built with single user access to several different providers as its main focus.

##Quickstart
To use `loudmoauth` with Leiningen or Boot include `[loudmoauth.core "0.1.1"]`.

Require `loudmoauth` in your application:
```Clojure
(ns my-app.core
  (:require [loudmoauth.core :as lmoauth]))
```
Start with creating a map with the specific parameters regarding your oauth2 service provider, here Spotify is used an example. Note that I've stashed the client id and secret in system variables but that is up to the user:
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
     "/oauth2" (lmoauth/parse-params request)
      "/interact"  (ringr/redirect (lmoauth/user-interaction))  
    {:status 200
     :body (:uri request)}))
```
Pass the map specified earlier as an argument to the `add-provider` function.
```Clojure
(lmoauth/add-provider spotify-oauth2-params)
```
Now visit the url that will trigger a call to `lm/user-interaction`, in our case `http://localhost:3000/interact`. If everything has worked as expected on the provider side you should now see the providers authentication page. Authenticate with the provider and they should provide you with the oauth2 tokens.

To retrieve your token call the `oauth-token` function with the keyword for the provider that you specified in your parameter map earlier:
```Clojure
(lmouath/oauth-token :spotify)
```
This should be it. For a more detailed explanation see below. For working examples see the repository [loudmoauth-examples](https://github.com/blmstrm/loudmoauth-examples).

##A bit more detail
###oauth-params map
To configure each provider one needs to provide a map of parameters describing the oauth2 service. They are as follows:

`:base-url` defines the root of the url of where our oauth2 api is located.  

`:auth-endpoint` defines the part that has to be added to the `:base-url` create the full authorization url.

`:token-endpoint` defines the part that to be added to the `:base-url` to create the full token retrival url
.
`:client-id ` defines the client ID received when registrating your application with the provider.

`:redirect-uri` defines the callback url to which the provider should make a http request when the user has authenticated through the provider.

`:scope` defines the rights your appplication is requesting from the user. This has to be a string where the different rights are separated by space.

`:custom-query-params` an optional key that defines the custom query parameters that some providers use to enable behaviour specific to their oauth2 service.

`:client-secret ` defines the client secret received when registrating your application with the provider.

`:provider` defines an arbitrary name to help you identify this provider and it's tokens.

###When/If the token expires
When a new provider is added to the client a separate thread will try to retrieve a new token when the current one is set to be expired. If, by coincidence, a request is sent to the API with the old token just as it expires the remote host will reply with a 403 http error message. To solve this it is a good idea to call the function `refresh-token`, if you receive a 403 response, to force a refresh and then try the API request again.
```Clojure
(lmoauth/refresh-token :spotify)
```

###Removing a provider 
To remove a provider call the `delete-provider` function with the provider keyword as an argument.
```Clojure
(lmoauth/delete-provider :spotify)
```
## License
The MIT License (MIT)

	Copyright (c) 2016 Karl Blomström

	Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:

	The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.

	THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
