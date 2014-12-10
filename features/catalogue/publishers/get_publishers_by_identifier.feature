@publishers @data_dependent @catalogue
Feature: Get publishers by identifier
  As an API consumer
  I want to be able to get multiple publishers by identifier
  So that I can efficiently display the information for specific publishers

  @smoke @CP-1241
  Scenario: Getting multiple publishers by identifier
    Given there are two publishers, each of which has books available for purchase
    When I request the publishers by identifiers
    Then the response is a list containing two publishers
    And the identifiers are present as specified

  @CP-1241
  Scenario: Getting multiple publishers by identifier, with a nonexistent identifier
    Given there are two publishers, each of which has books available for purchase
    When I request these publishers and a publisher which does not exist, by identifiers
    Then the response is a list containing two publishers
    And the identifiers are of existent publishers

  @CP-1241 @CP-1242
  Scenario: Getting multiple publishers by identifier, with all nonexistent identifiers
    When I request two publishers that do not exist, by identifiers
    Then the response is a list containing no publishers

  @smoke @data_dependent
  Scenario: Getting multiple publishers by identifier is publicly cacheable
    Given there are two publishers, each of which has books available for purchase
    When I request the publishers by identifiers
    Then the response is publicly cacheable
