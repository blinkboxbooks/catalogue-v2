@search @search_results

Feature: Search results
  As an application
  I want to be able to search for books
  So that users can find books they wish to buy

  @smoke @data_dependent
  Scenario: Search for books with valid author name
    Given the search query is "David Ovason"
    When I search for the query
    Then the response is a list containing at least ten books
    And it is publicly cacheable
    And the first book's author is "David Ovason"

  @smoke  @data_dependent
  Scenario: Search for books with valid book title
    Given the search query is "The Godfather"
    When I search for the query
    Then the response is a list containing at least ten books
    And the first book's title is "The Godfather"

  @smoke  @data_dependent
  Scenario: Search for book with exact ISBN
    Given the search query is "9781448106899"
    When I search for the query
    Then the response is a list containing only one book
    And the first book's title is "The Godfather"
    And the first book's author is "Mario Puzo"

  @data_dependent
  Scenario: Valid search for books with maximum count
    Given I want a maximum of ten results
    And the search query will return many results
    When I search for the query
    Then the response is a list containing only ten books

  @data_dependent
  Scenario: Valid search for books with maximum count and an offset
    Given the search query will return many results
    And I want a maximum of ten results at offset five
    When I search for the query
    Then the response is a list containing ten books

  @cp-210  @data_dependent
  Scenario: Search for books with multiple authors
    Given there is at least one book which has multiple authors
    When I search for the book by its isbn
    Then the response is a list containing only one book
    And multiple author names are returned

  @cp-225 @data_dependent
  Scenario: Search for books with valid generic word
    Given the search query is "the"
    And I want a maximum of ten results
    When I search for the query
    Then the response is a list containing only ten books
    And each book's content contains "the"

  @data_dependent
  Scenario: Zero results search with valid generic word
    Given the search query is "somesearchtermthatwillneverhit"
    When I search for the query
    Then the response is a list containing zero books

  @cp-213 @smoke
  Scenario Outline: Invalid search with special character
    Given the search query is "<term>"
    When I search for the invalid query
    Then the request fails because it was invalid

    Examples:
      | term |
      | *    |
      | ?    |
      | $    |
      | $    |
      | £    |
      |      |
