@contributors @get_contributors @data_dependent
Feature: Get contributors by identifier
  As an API consumer
  I want to be able to get multiple contributors by identifier
  So that I can efficiently display the information for specific contributors

  @smoke @CP-1241
  Scenario: Getting multiple contributors by identifier
    Given there are two contributors, each of which is an author
    When I request the contributors by identifiers
    Then the response is a list containing two contributors
    And the identifiers are present as specified

  Scenario: Getting multiple contributors by identifier, with a nonexistent identifier
    Given there are two contributors, each of which is an author
    When I request these contributors and a contributor which does not exist, by identifiers
    Then the response is a list containing two contributors
    And the identifiers are of existent contributors

  @CP-1241 @CP-1242
  Scenario: Getting multiple contributors by identifier, with all nonexistent identifiers
    When I request two contributors that do not exist, by identifiers
    Then the response is a list containing no contributors

  @smoke
  Scenario: getting multiple contributors is publicly cacheable
    Given there are two contributors, each of which is an author
    When I request these contributors by identifiers
    Then the response is publicly cacheable