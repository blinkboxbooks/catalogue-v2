@contributors @get_contributor
Feature: Get a contributor by identifier
  As an API consumer
  I want to be able to get a contributor by identifier
  So that I can display the information for a specific contributor

  @smoke @data_dependent
  Scenario: Getting a contributor by identifier
    Given there is a contributor which is an author
    When I request that contributor by identifier
    Then the response is a contributor
    And it has the following attributes:
      | attribute    | type    | description                                             |
      | display name | String  | The name of the contributor to display                  |
      | sort name    | String  | The name to use for sorting contributors                |
      | book count   | Integer | The number of books the contributor has on our platform |
    And the identifier is the one specified

  Scenario: Trying to get a contributor with a nonexistent identifier
    When I request a contributor by identifier, which does not exist
    Then the request fails because the contributor was not found

  Scenario: Trying to get a contributor with a non-alphanumeric identifier
    When I request the contributor by invalid identifier
    Then the request fails because the contributor was not found

  @smoke @data_dependent
  Scenario: Getting a contributor by identifier is publicly cacheable
    Given there is a contributor which is an author
    When I request that contributor by identifier
    Then the response is publicly cacheable
