@catalogue @books @data_dependent
Feature: Get books by identifier
  As an API consumer
  I want to be able to get multiple books using identifiers
  So that I can efficiently display the information for specific books

  @smoke @CP-1241
  Scenario: Getting multiple books by identifier are returned in the order they are requested
    Given there are three books, each of which is currently available for purchase
    When I request these books by identifiers
    Then the response is a list containing three books
    And the identifiers are present and in order, as specified

  @smoke
  Scenario: Multiple books retrieved by identifier are publicly cacheable
    Given there are three books, each of which is currently available for purchase
    When I request these books by identifiers
    Then the response is publicly cacheable

  @CP-1241
  Scenario: Getting multiple books by identifier, with a nonexistent identifier
    Given there are two books, each of which is currently available for purchase
    When I request these books and a book which does not exist, by identifiers
    Then the response is a list containing two books
    And the identifiers are of existent books

  @CP-1241 @CP-1242
  Scenario: Getting multiple books by identifier, with all nonexistent identifiers
    When I request two books that do not exist, by identifiers
    Then the response is a list containing no books