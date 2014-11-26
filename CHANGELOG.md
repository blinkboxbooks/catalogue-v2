

## 0.1.3 ([#13](https://git.mobcastdev.com/Agora/catalogue-v2/pull/13) 2014-11-26 13:55:10)

CAT-29 Search books by title

### Improvements

* Implement E2E tests aiming to ensure the correct order for title matches in book search
* Introduce a computed field that uses a simpler analyser which doesn't strip stop-words for the title
* Use the stop-word-aware title field in the search query so that it takes precedence over the stop-word-removing one


## 0.1.2 ([#12](https://git.mobcastdev.com/Agora/catalogue-v2/pull/12) 2014-11-26 10:45:03)

CAT-52: V1 XML messages parser

patch

- parsing mechanism for 'book', 'undistribute' and 'book-price' xml message types
- unit tests for different missing fields on book/undistribute/book-price xmls
- fork the running of the tests in order to facilitate ease of running them within sbt (without the need of restarting it after few runs, due to the out of permgen mem)

## 0.1.1 ([#10](https://git.mobcastdev.com/Agora/catalogue-v2/pull/10) 2014-11-21 16:54:45)

E2E test setup

### Improvements

This PR introduces an infrastructure and a DSL that enables end-to-end testing on elastic-search based services. It also provides a couple of examples of usage as already-implemented tests.

## 0.1.0 ([#9](https://git.mobcastdev.com/Agora/catalogue-v2/pull/9) 2014-11-21 09:46:18)

CAT-51: Ingester v1 and v2 submodules.

new feature
- create new v1 ingester submodule
- improve the books model
- fix consumers of the new books model in order to use the new interface

