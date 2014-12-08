@contributor_groups @get_contributor_groups
Feature: Get a contributor group
  As an API consumer
  I want to be able to get contributor groups
  So that I can display the information for a specific contributor group

  @data_dependent
  Scenario: Getting a contributor group by identifier
    Given there is a contributor group which is currently active
    When I request that contributor group by identifier
    Then the response is a contributor group
    And it has the following attributes:
      | attribute    | type   | description                                  |
      | name key     | String | The name key of the contributor group        |
      | display name | String | The name of the contributor group to display |
      | start date   | Date   | The start date the contributor group         |
      | end date     | Date   | The end date the contributor group           |
    And the identifier is the one specified

  Scenario: Trying to get a contributor group with a non-existent identifier
    Given there is a contributor group which does not exist
    When I request a contributor group by identifier
    Then the request fails because the contributor group was not found

  Scenario: Trying to get a contributor with a non-alphanumeric identifier
    When I request a contributor group by invalid identifier
    Then the request fails because the contributor group was not found