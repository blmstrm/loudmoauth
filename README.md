[![Build Status](https://travis-ci.org/blmstrm/loudmoauth.svg?branch=master)](https://travis-ci.org/blmstrm/loudmoauth)
[![Dependencies Status](https://jarkeeper.com/blmstrm/loudmoauth/status.svg)](https://jarkeeper.com/blmstrm/loudmoauth)
# Loudmoauth
Loudmoauth is ment to be a library for managing ouath2 client tokens independently of what service one is using.

## Usage

###Set the oauth2 parameters `set-oauth-params`
Supply a map with the following keys:
  ```Clojure
  {:base-url "https://www.example.com"
  :client-id "34jfkdl3...4fjdl2"
  :redirect-uri "https://www.example.com/callback"
  :scope "do-one-thing do-another-thing"
  :custom-query-params {:likes-cake "Yes"}
  :client-secret "23dj2k3k23kd...2312323s2s"}
```
as an argument to the `set-oauth-params` function.

###Retrieve your token
To retrieve your token call the `token` function.

#### Custom query parameters `:custom-query-params`
If the oauth2 service provider demands or gives you the possibility of any custom query parameters please include them in the map associated with the key `:custom-query-params`. This key is optional, so feel free to leave it out.



### When the token expires
Loudmoauth tries to update your token for you whenever the token has reached its expiry time.

## License
The MIT License (MIT)

Copyright (c) 2016 Karl Blomström

Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
