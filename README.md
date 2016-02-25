# Loudmoauth

`Loudmoauth` is ment to be a library for managing ouath2 client tokens independt of what service one is using.

## Usage
Supply a `map` with the following parameters:
  ```Clojure
  {:base-url "https://www.example.com"
  :client-id "34jfkdl3...4fjdl2"
  :redirect-uri "https://www.example.com/callback"
  :scope "do-one-thing do-another-thing"
  :custom-query-params {:likes-cake "Yes"}
  :client-secret "23dj2k3k23kd...2312323s2s"}
```
as an argument to the `set-oauth-params` function.

To retrieve your token call the `token` function.

### Custom query parameters
Some services gives the client the ability to include custom query parameters. If you need this include them in themap associated with they key `:custom-query-params`.  

### When the token expires
Loudmoauth tries to update your token for you whenever the token has reached its expiry time.

## License
The MIT License (MIT)

Copyright (c) 2016 Karl Blomström

Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
