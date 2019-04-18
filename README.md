# flow-bot

A program designed to keep automatically in sync a main repositor with an open source one, which consits of a single folder of the main repo.

The program needs the following environment variables, supplied, for example, with a `.lein-env` file

```clojure
{:org "test-org-integration"
 :server-repo "test-app"
 :client-repo "client-app"
 :client-folder "client"
 :auth "XXXX"
 :magic-string "OK TO MERGE"}
```
