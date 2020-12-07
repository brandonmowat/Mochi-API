# Mochi API
The API that powers a Mochi blog.

## Build

To rebuild your executable, just run:
`clj -M:uberjar`

Then deploy to heroku (or whatever else)
`git push heroku master`

#### Why do we have an empty `project.clj` file?

We have this empty file so that heroku know's that it's a clojure project.
