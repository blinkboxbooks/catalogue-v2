@contributors @get_contributors @data_dependent
Feature: Get contributors by group name
  As an API consumer
  I want to be able to get multiple contributors by contributor group name
  So that I can efficiently display the information for specific contributor groups

  @CP-1241
  Scenario: Getting multiple contributors by contributor group name
    Given there is a contributor group which is currently active
    When I request the contributors, by group name
    Then the response is a list containing at least one contributor

  @CP-1241 @CP-1242
  Scenario: Getting multiple contributors by nonexistent group name
    When I request the contributors, by group name which does not exist
    Then the response is a list containing no contributors
