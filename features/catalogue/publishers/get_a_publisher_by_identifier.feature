@publishers @catalogue
Feature: Get a publisher by identifier
  As an API consumer
  I want to be able to get a publisher by identifier
  So that I can display the information for a specific publisher

  @smoke @data_dependent
  Scenario: Getting a publisher by identifier
    Given there is a publisher which has books available for purchase
    When I request that publisher by identifier
    Then the response is a publisher
    And the identifier is the one specified
    And it has the following attributes:
      | attribute    | type    | description                                           |
      | display name | String  | The name of the publisher                             |
      | book count   | Integer | The number of books the publisher has on our platform |

  Scenario: Trying to get a publisher with a nonexistent identifier
    When I request the publisher by identifier, which does not exist
    Then the request fails because the publisher was not found

  Scenario: Trying to get a publisher with a non-alphanumeric identifier
    When I request the publisher by invalid identifier
    Then the request fails because the publisher was not found

  @smoke @data_dependent
  Scenario: Getting a publisher by identifier is publicly cacheable
    Given there is a publisher which has books available for purchase
    When I request the publisher by identifier
    Then the response is publicly cacheable
