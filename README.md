# flow-bot

A program designed to keep automatically in sync a main repositor with an open source one, which consits of a single folder of the main repo.

The program needs the following environment variables, supplied, for example, with a `.lein-env` file

```clojure
{:server-org    "workshub"
  :server-repo   "test-app"
  :server-git    "XXX"
  :client-org    "test-org-integration"
  :client-repo   "client-app"
  :client-git    "XXX"
  :client-folder "client"
  :auth          "XXX"
  :magic-string  "OK TO MERGE"}
```
