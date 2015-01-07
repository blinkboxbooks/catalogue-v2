

## 0.1.66 ([#80](https://git.mobcastdev.com/Agora/catalogue-v2/pull/80) 2015-01-07 09:43:50)

Bulk indexing using HTTP ES client.

Patch

- Bulk indexing implementation using the new HTTP ES client

## 0.1.65 ([#79](https://git.mobcastdev.com/Agora/catalogue-v2/pull/79) 2015-01-06 16:16:10)

Introduce ES HTTP Client

Patch

- update dependencies to the latest versions (where possible)
- code cleanup (removing commented out code)

## 0.1.64 ([#78](https://git.mobcastdev.com/Agora/catalogue-v2/pull/78) 2015-01-05 10:57:24)

Improve routes readability

### Improvements

Make routes a little more readable in the search-api  by factoring out the route from the URI matching and some complex validation.

## 0.1.63 ([#77](https://git.mobcastdev.com/Agora/catalogue-v2/pull/77) 2014-12-19 15:11:01)

Split specs to be more easier to digest for CI

### Improvements

Split the BasicSearchSpecs to be less resource-hungry and hopefully reduce transient failures on CI.

## 0.1.62 ([#76](https://git.mobcastdev.com/Agora/catalogue-v2/pull/76) 2014-12-19 14:20:29)

Cat 134 - sorting books by contributor by title

### bugfix
- Added multi-field for sorting by title


## 0.1.61 ([#75](https://git.mobcastdev.com/Agora/catalogue-v2/pull/75) 2014-12-19 13:48:34)

CAT-138 Support partial matches for the author field

### Improvements

Introduce partial match support for author names

## 0.1.60 ([#70](https://git.mobcastdev.com/Agora/catalogue-v2/pull/70) 2014-12-19 11:33:51)

CAT-139 Introduce spelling suggestions for mis-spelled authors

### Improvements

* Fix suggestions appearing for correctly spelled authors
* Provide suggestions for mis-spelled authors

## 0.1.59 ([#71](https://git.mobcastdev.com/Agora/catalogue-v2/pull/71) 2014-12-19 10:45:33)

Acceptance tests

#### Test improvement
* Add tests for checking existence of this, next and prev links in a search results response

## 0.1.58 ([#72](https://git.mobcastdev.com/Agora/catalogue-v2/pull/72) 2014-12-18 18:08:14)

CAT-136 Fix spelling suggestions so that multiple words are supported

### Improvements

Support more mis-spelled words in a query

## 0.1.57 ([#73](https://git.mobcastdev.com/Agora/catalogue-v2/pull/73) 2014-12-18 17:55:54)

CAT-137 Remove suggestions from the response when no suggestions are provided

### Improvements

Fix search responses by removing the `suggestions` element when no suggestions are returned by the index.

## 0.1.56 ([#69](https://git.mobcastdev.com/Agora/catalogue-v2/pull/69) 2014-12-18 17:42:18)

CAT-103: Keep book metadata when undistributing.

Patch

- use a child document for undistribute messages, having the book as the
  parent;
- update the search service in order to use the child document;
- retry indexing a message when some ES communication is happening;

## 0.1.55 ([#65](https://git.mobcastdev.com/Agora/catalogue-v2/pull/65) 2014-12-18 11:43:34)

Cat 134 - search and catalogue have separate sort-order mappings

### bugfix
- Search and Catalogue components have separate sort-order mappings.
- Removed superfluous sort-by-title in search.
- Duplicated permitted values in API and ES layers (not pretty but works).

## 0.1.54 ([#67](https://git.mobcastdev.com/Agora/catalogue-v2/pull/67) 2014-12-18 11:08:50)

CAT-131: Removed links from related books end-point inline with v1

### Patch


## 0.1.53 ([#68](https://git.mobcastdev.com/Agora/catalogue-v2/pull/68) 2014-12-18 10:44:35)

Spelling suggestion acceptance tests

#### Test improvement
* Add more misspelling scenarios
* Slight rewording of steps
* Change assertion to be_empty / be_nil

## 0.1.52 ([#66](https://git.mobcastdev.com/Agora/catalogue-v2/pull/66) 2014-12-17 16:30:56)

Use fanout exchange type instead of headers.

Patch

- Use fanout exchange type instead of headers.

## 0.1.51 ([#64](https://git.mobcastdev.com/Agora/catalogue-v2/pull/64) 2014-12-16 17:35:41)

Use defs instead of vals for exception and rejection handlers.

Patch

Use defs instead of vals for exception and rejection handlers.


## 0.1.50 ([#63](https://git.mobcastdev.com/Agora/catalogue-v2/pull/63) 2014-12-16 14:52:46)

Cat 134

### bugfix
- Related books and similar books end-points should have same order of results.
- Fixed sorting by title.
- Moved common MLT query.
- Tidied up sorting unit-tests.


## 0.1.49 ([#62](https://git.mobcastdev.com/Agora/catalogue-v2/pull/62) 2014-12-15 17:54:45)

Introduce timeout for ES client and log unhandled failures

### Improvements

* Introduce a more reasonable, configurable timeout when connecting to ES
* Log un-handled failures at the API level

## 0.1.48 ([#60](https://git.mobcastdev.com/Agora/catalogue-v2/pull/60) 2014-12-15 16:44:54)

CAT-122 Implement spelling suggestions

### Improvements

Implement spellcheck suggestions for the search endpoint

## 0.1.47 ([#59](https://git.mobcastdev.com/Agora/catalogue-v2/pull/59) 2014-12-15 16:21:40)

Fixed path prefix in links for related books

### bugfix


## 0.1.46 ([#58](https://git.mobcastdev.com/Agora/catalogue-v2/pull/58) 2014-12-15 14:51:26)

CAT-125: Links in search results always contain count and offset

### Patch


## 0.1.45 ([#55](https://git.mobcastdev.com/Agora/catalogue-v2/pull/55) 2014-12-15 11:59:05)

Cat 45 - relaxed dateFilter() usage

### Patch
- Modified `dateFilter` helper so we can use it in combination with other filters.

## 0.1.44 ([#57](https://git.mobcastdev.com/Agora/catalogue-v2/pull/57) 2014-12-15 11:19:14)

Update dependencies

#### Test improvements
* Update dependencies
* Require all from cucumber/blinkbox instead of individual modules

## 0.1.43 ([#56](https://git.mobcastdev.com/Agora/catalogue-v2/pull/56) 2014-12-12 17:42:56)

Fix README in order to use the new module names.

Patch

## 0.1.42 ([#54](https://git.mobcastdev.com/Agora/catalogue-v2/pull/54) 2014-12-12 15:48:08)

Improve search tests

#### Test improvement
* Make search tests use set_query_param from cucumber-blinkbox instead of setting @params
* remove "I have specified a sort order of " step_def from search step defs as the same step exists in catalogue step_defs

## 0.1.41 ([#53](https://git.mobcastdev.com/Agora/catalogue-v2/pull/53) 2014-12-12 15:36:27)

Enable graylog logging for marvin1 ingester.

Patch

- send logs to graylog by mixing 'Loggers'

## 0.1.40 ([#50](https://git.mobcastdev.com/Agora/catalogue-v2/pull/50) 2014-12-12 15:22:32)

Cat 45 - v1 search sorting and ordering

### Patch
- search service matches existing v1 sorting and ordering
- added common sorting, pagination, validation to `common` package


## 0.1.39 ([#51](https://git.mobcastdev.com/Agora/catalogue-v2/pull/51) 2014-12-12 14:44:06)

CAT-124 Switched links to be relative in line with v1 api

#### Patch

* Switched links to be relative in line with v1 api


## 0.1.38 ([#49](https://git.mobcastdev.com/Agora/catalogue-v2/pull/49) 2014-12-12 11:34:47)

Fix the internal (rpm) names.

Patch

- change the rpm name of the submodules.

## 0.1.37 ([#48](https://git.mobcastdev.com/Agora/catalogue-v2/pull/48) 2014-12-12 10:36:30)

Graylog configuration

Patch

- change logging config in order to send the logs to Graylog
- remove swagger config from catalogue2-service-public (as not being used)

## 0.1.36 ([#47](https://git.mobcastdev.com/Agora/catalogue-v2/pull/47) 2014-12-11 17:14:08)

Project modules renaming

Patch

Quite a big renaming
- rename modules in order to differentiate from the older catalogue service(s)

## 0.1.35 ([#46](https://git.mobcastdev.com/Agora/catalogue-v2/pull/46) 2014-12-11 16:57:08)

CAT-123 Improve similar authors to use similar logic as the search-service

### Improvements

Take inspiration from [the old search-service](https://git.mobcastdev.com/Labs/spray-search-service/blob/master/src/main/resources/solr/books/conf/solrconfig.xml#L115) to provide a more-like-this relevance which is similar to the existing one.

## 0.1.34 ([#44](https://git.mobcastdev.com/Agora/catalogue-v2/pull/44) 2014-12-11 13:56:07)

Add more cache checks for search api

#### Test improvement
* Add cache checks for suggestions, spelling and similar endpoint

## 0.1.33 ([#41](https://git.mobcastdev.com/Agora/catalogue-v2/pull/41) 2014-12-11 11:55:18)

CAT-111 Ensure no `books` array is returned if no book is matched

### Improvements

This patch makes the response of *search* and *similar* APIs more compliant with v1 removing the `books` array when no books are matched.

## 0.1.32 ([#40](https://git.mobcastdev.com/Agora/catalogue-v2/pull/40) 2014-12-11 11:48:02)

Cat 46

### Patch
- Caching directives to all end-point methods to match existing v1 search service
- Configuration class and factored out existing properties
- Support for console logging

## 0.1.31 ([#42](https://git.mobcastdev.com/Agora/catalogue-v2/pull/42) 2014-12-11 10:47:39)

CAT-108 - Fix query special character handling

### Improvement

For V1 compliance:

* Introduce more checks in terms of query validation and pre-processing
* Make responses for validation errors not JSON-encoded

## 0.1.30 ([#33](https://git.mobcastdev.com/Agora/catalogue-v2/pull/33) 2014-12-10 16:49:57)

Migrate search service cucumber tests

#### Test improvement
 Tests have been migrated from: https://git.mobcastdev.com/Platform/search-services
* Migrate search-service tests
* Migrate distributor messages which act as test fixtures
* Migrate data.yml

* Re-word steps so they don't clash with catalogue tests
* Include zero in transforms
* Updated environment config
* Add instructions for running tests locally
* Fixed a search step which was searching for a hash of the search term

## 0.1.29 ([#39](https://git.mobcastdev.com/Agora/catalogue-v2/pull/39) 2014-12-10 15:10:09)

Move some leftover tests from the old structure to the new one.

Patch

- there was a leftover test which needed to be moved to the new folder.

## 0.1.28 ([#38](https://git.mobcastdev.com/Agora/catalogue-v2/pull/38) 2014-12-10 14:58:41)

CAT-107 Ensure empty searches result in the correct 400 response

### Bugfix

* Introduce missing akka log configuration (not really part of the ticket)
* Ensures the correct responses are returned in case of an empty search query

## 0.1.27 ([#35](https://git.mobcastdev.com/Agora/catalogue-v2/pull/35) 2014-12-10 14:17:44)

CAT-32: Modules renaming

Patch:

- ingesterv1 -> catalogue-ingester-marvin1
- ingesterv2 -> catalogue-ingester-marvin2
- searchv1 -> catalogue-search-service

## 0.1.26 ([#37](https://git.mobcastdev.com/Agora/catalogue-v2/pull/37) 2014-12-10 13:48:03)

CAT-116 Introduce validation for ISBN in similar books endpoint

### Improvements

Fixes missing validation checks on the ISBN part of a similar-books URL.

## 0.1.25 ([#36](https://git.mobcastdev.com/Agora/catalogue-v2/pull/36) 2014-12-10 13:28:46)

Cat 16

### Patch
- Refactored unit-test to create index once

## 0.1.24 ([#34](https://git.mobcastdev.com/Agora/catalogue-v2/pull/34) 2014-12-10 11:24:14)

CAT-114 Implement pagination links in search and similar responses

### Improvement

* Implement the pagination links for search and similar responses.

## 0.1.23 ([#32](https://git.mobcastdev.com/Agora/catalogue-v2/pull/32) 2014-12-10 09:58:21)

Added health check end-point

### Patch
- Added health check end-point

## 0.1.22 ([#31](https://git.mobcastdev.com/Agora/catalogue-v2/pull/31) 2014-12-09 16:38:46)

CAT-113 Introduce number of results in similar books

### Bugfix

* Add the missing `numberOfResults` field in the response for the similar books endpoint
* Introduce helper to write end-to-end tests creating the index only once per test-suite

## 0.1.21 ([#30](https://git.mobcastdev.com/Agora/catalogue-v2/pull/30) 2014-12-09 13:49:59)

Fix name of searchv1 project name in build.sbt

### Improvements

Fix name of searchv1 project in sbt build.

## 0.1.20 ([#29](https://git.mobcastdev.com/Agora/catalogue-v2/pull/29) 2014-12-09 10:22:32)

Cat 16

### Patch
- Implemented date-range filtering
- Minor changes following previous review

## 0.1.19 ([#27](https://git.mobcastdev.com/Agora/catalogue-v2/pull/27) 2014-12-08 14:51:29)

Migrate catalogue-service cucumber tests to V2 repository

#### Test improvement
From https://git.mobcastdev.com/Agora/catalogue-service
* Migrate features folder (contains features, step_defs, and support code)
* Migrate config folder (contains data dependencies, environment addresses, cucumber profiles)
* Migrate Gemfile, Gemfile.lock and Rakefile

## 0.1.18 ([#28](https://git.mobcastdev.com/Agora/catalogue-v2/pull/28) 2014-12-08 11:11:20)

Cat 16

### Patch
- Added pagination and sorting
- Added all v1 links to responses
- Completed DAO for v1 end-points
- Reinstated health check end-point
- Improved code-coverage for service unit-tests
- Added adapter for creating `linkHelper`
- Moved all constants into service layer
- Removed all redundant code
- General code tarting up

## 0.1.17 ([#26](https://git.mobcastdev.com/Agora/catalogue-v2/pull/26) 2014-12-02 14:44:21)

CAT-24 - More tests on pagination

### Improvements

* Introduce tests and fixes on pagination of suggestions
* Introduce tests and fixes on pagination of MoreLikeThis
* Increase the patience configuration for `ApiSpecBase` in order to avoid failing on CI

## 0.1.16 ([#24](https://git.mobcastdev.com/Agora/catalogue-v2/pull/24) 2014-12-02 13:53:28)

Cat 54

### Patch
- Implemented API spray layer for `/book/?contributor=xxx` method
- Additional unit-tests for API layer
- Removed some redundant code (more to do)
- Tidied up API unit-test class a little

## 0.1.15 ([#23](https://git.mobcastdev.com/Agora/catalogue-v2/pull/23) 2014-12-02 13:20:59)

Fix xml parsing issues

Patch
- add text trimming to the xml text nodes
- make all the fields optional for Source, except 'username'
- proper parsing of different fields like regions, prices, descriptions
- update/add unit tests in order to cover different parsing scenarios
- use different queue names in order to get the messages being published to the distribution exchanges

## 0.1.14 ([#25](https://git.mobcastdev.com/Agora/catalogue-v2/pull/25) 2014-12-02 11:29:41)

CAT-77 Increase sloppines to better match word permutations

### Bugfix

Increase the sloppiness of the title/author/description queries in order to match documents where the word order is permuted with respect to the query.

## 0.1.13 ([#22](https://git.mobcastdev.com/Agora/catalogue-v2/pull/22) 2014-12-01 17:03:58)

CAT-24 Implement pagination and response wrappers for compatibility with existing v1 APIs

### Improvements

* Improve the response format to be compatible with the one of the existing v1 APIs
* Implement pagination

## 0.1.12 ([#21](https://git.mobcastdev.com/Agora/catalogue-v2/pull/21) 2014-12-01 16:47:44)

CAT-68 Include ISBN as a criteria for book search

### Improvements

This patch fixes the search endpoint as it wasn't matching on the ISBN field; now books can be found by ISBN as well.

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

