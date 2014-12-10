@publishers @catalogue
Feature: List publishers
  As an API consumer
  I want to be able to list publishers
  So that I can allow users to choose a publisher they are interested in

  @smoke
  Scenario: Listing all publishers
    Given there is a publisher which has books available for purchase
    When I request all publishers
    Then the response is a list containing at least one publisher

  Scenario: Listing a maximum number of publishers
    Given there are two publishers, each of which has books available for purchase
    When I request all publishers, using the following filters:
      | count     | 2       |
    Then the response is a list containing two publishers

  Scenario: Listing a maximum number of publishers, at an offset
    When I request all publishers, using the following filters:
      | count     | 5       |
      | offset    | 10      |
    Then the response is a list containing 5 publishers at offset 10

  Scenario: Trying to list publishers with a negative count
    When I request all publishers, using the following filters:
      | count     | -1      |
    Then the request fails because it is invalid

  Scenario: Trying to list publishers with a negative offset
    When I request all publishers, using the following filters:
      | count     | 10      |
      | offset    | -1      |
    Then the request fails because it is invalid

  Scenario: Listing all publishers is publicly cacheable
    When I request all publishers
    Then the response is publicly cacheable

  Scenario Outline: Listing publishers in name order
    Given I have specified a sort order of <order>, <direction>
    When I request all publishers, using the following filters:
      | count     | 10    |
    Then the response is a list containing ten publishers in the Display Name order

  Examples:
    | order | direction  |
    | Name  | ascending  |
    | Name  | descending |