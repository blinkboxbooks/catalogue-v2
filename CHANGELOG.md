

## 0.1.11 ([#18](https://git.mobcastdev.com/Agora/catalogue-v2/pull/18) 2014-12-01 16:39:55)

Add RPM/gpg/fatjar capabilities.

Patch
- RPM/gpg/fatjar capabilities

## 0.1.10 ([#20](https://git.mobcastdev.com/Agora/catalogue-v2/pull/20) 2014-12-01 16:17:39)

CAT-66 Use the correct marshallers to support Accept headers

### Bugfix

This patch fixes the v1 search API to support the correct content-types and Accept headers.

## 0.1.9 ([#16](https://git.mobcastdev.com/Agora/catalogue-v2/pull/16) 2014-12-01 11:07:37)

Cat 54

### Patch
- Added DAO unit-test
- Couple of review fixes that didn't make last PR
- Added framework for get bulk books API method

## 0.1.8 ([#19](https://git.mobcastdev.com/Agora/catalogue-v2/pull/19) 2014-11-28 17:39:42)

s/search/elasticsearch in config

### Improvements

Improve the configuration by renaming the too generic `search` to a more specific `elasticsearch`.

## 0.1.7 ([#17](https://git.mobcastdev.com/Agora/catalogue-v2/pull/17) 2014-11-28 15:24:58)

:s/browser/searchv1/g

### Improvements

This patch makes the project structure a bit more uniform by renaming the "browser" submodule to "searchv1" thus communicating the purpose of being a replacement for the old search APIs.

## 0.1.6 ([#15](https://git.mobcastdev.com/Agora/catalogue-v2/pull/15) 2014-11-27 15:25:08)

CAT-62: Update messaging configuration

Patch
- having two messaging configurations: book and book-price
- create two different messaging consumers, one for each type of message
- split the xml parsers in order to have different parsing strategies for each message type.

## 0.1.5 ([#14](https://git.mobcastdev.com/Agora/catalogue-v2/pull/14) 2014-11-27 11:15:35)

CAT-59: Index the parsed/validated requests

Patch
- create a new mappings for 'book-price' messages
- parse 'undistribute' reasons messages in order to be indexed next to the book
- refactor the indexer service in order to accept the distribution supertype instead of the concrete implementations

## 0.1.4 ([#11](https://git.mobcastdev.com/Agora/catalogue-v2/pull/11) 2014-11-26 15:27:45)

Cat 58

### Patch
- Unit-tests for /books API prototype
- Separated ES and service code

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

