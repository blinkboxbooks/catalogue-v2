@categories @list_categories
Feature: List categories
  As an API consumer
  I want to be able to list categories
  So that I can allow users to choose a category they are interested in

  Scenario: Listing a maximum number of categories
    When I request all categories, using the following filters:
      | count     | 3      |
    Then the response is a list containing three categories in the Sequence order

  Scenario: Listing a maximum number of categories, at an offset
    When I request all categories, using the following filters:
      | count     | 5       |
      | offset    | 10      |
    Then the response is a list containing 5 categories at offset 10

  Scenario: Trying to list categories with a negative count
    When I request all categories, using the following filters:
      | count     | -1      |
    Then the request fails because it is invalid

  Scenario: Trying to list categories with a negative offset
    When I request all categories, using the following filters:
      | count     | 10      |
      | offset    | -1      |
    Then the request fails because it is invalid

  @smoke
  Scenario: Categories are publicly cacheable
    When I request all categories
    Then the response is publicly cacheable