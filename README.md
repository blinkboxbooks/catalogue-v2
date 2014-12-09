# Catalogue V2

The new blinkbox-books catalogue

## Running the cucumber acceptance tests locally

### Pre-requisites
* elasticsearch installed and up and running, configure cluster name here: [reference.conf](common/src/main/resources/reference.conf)
* Rabbit-MQ (can be configured in [environments.yml](features/support/config/environments.yml) (only needed to load the test fixtures)

### Test fixtures
Test data required to run these tests lives in: [fixtures](features/support/fixtures/)

Start the ingester
```
$ sbt 'project ingesterv1' run
```
Feed the messages into the input queue configured: [reference.conf](ingesterv1/src/main/resources/reference.conf)

### Starting the apps and running the tests
First start the services:
```
$ sbt 'project searchv1' clean run
$ sbt 'project catalogue' clean run
```

Then run the tests:
```
$ bundle install
$ bundle exec cucumber --tags @search   #for search tests
$ bundle exec cucumber --tags ~@search  #for catalogue tests
$ bundle exec cucumber                  #for all tests
```

## Running the cucumber acceptance tests against DevInt
```
$ bundle install
$ bundle exec cucumber  -p ci-smoke-dev-int
$ bundle exec cucumber  -p ci-regression-dev-int
```
