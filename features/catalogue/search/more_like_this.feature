@search @more_like_this
Feature: More Like This
  As an application
  I want to suggest similar books
  So that users buy more books

  @smoke
  Scenario: Similar Books for a book
    Given there is at least one book which has many similar books
    When I request similar books
    Then the search response is a list containing 10 books
    And it is publicly cacheable

  Scenario: Similar Books for a prolific author
    Given there is at least one book whose author has written others in the catalogue
    When I request similar books
    Then the first book's author is the same as the original book

  Scenario: Limit number of Similar Book results
    Given I want a maximum of 5 results
    And there is at least one book which can be searched for
    When I request similar books
    Then the search response is a list containing 5 books

  Scenario: Offset similar book results
    Given I want a maximum of 5 results at offset 12
    And there is at least one book which has many similar books
    When I request similar books
    Then the search response is a list containing 5 books at offset 12

  Scenario Outline: Bad Isbn provided
    Given an invalid isbn "<bad_data>"
    When I request similar books for the invalid isbn
    Then the request fails because it is invalid
  Examples:
    | bad_data      |
    | aaa           |
    | 42            |
    | thisisnotgood |
