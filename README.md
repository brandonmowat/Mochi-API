# Mochi API
The API that powers a Mochi blog.

last updated: June 27, 2023

## Build

To rebuild your executable, just run:
`clj -M:uberjar`

## Deploy

Then deploy to heroku (or whatever else)
`git push heroku master`

#### Why do we have an empty `project.clj` file?

We have this empty file so that heroku know's that it's a clojure project.
