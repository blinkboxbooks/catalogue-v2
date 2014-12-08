@contributor_groups @list_contributor_groups
Feature: List contributor_groups
  As an API consumer
  I want to be able to list contributor groups
  So that I have information on the groups and use the information to find groups I am interested in

  @CP-1503
  Scenario: Listing a maximum number of contributor groups
    When I request all contributor groups, using the following filters:
      | count     | 2      |
    Then the response is a list containing two contributor groups

  Scenario: Listing a maximum number of contributor groups, at an offset
    When I request all contributor groups, using the following filters:
      | count     | 2       |
      | offset    | 1       |
    Then the response is a list containing two contributor group at offset one

  Scenario: Trying to list contributor groups with a negative count
    When I request all contributor groups, using the following filters:
      | count     | -1      |
    Then the request fails because it is invalid

  Scenario: Trying to list contributor groups with a negative offset
    When I request all contributor groups, using the following filters:
      | count     | 10      |
      | offset    | -1      |
    Then the request fails because it is invalid