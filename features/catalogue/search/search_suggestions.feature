@search_suggestions
@search

Feature: Search suggestions
  As an application
  I want to display search suggestions for a query the user started to type
  In order to help users do a better search for the books

  Scenario Outline: Return list of suggested search terms based on the initial query provided, starting from a single character terms
    Given I have entered the query "<search_query>"
    When I request search suggestions for the query
    Then the search response is a list containing 10 search suggestions
    And each suggestion contains "<search_query>"

    Examples:
      |    search_query   |
      |      G            |
      |      Ga           |
      |      Gat          |
      |      Gats         |
      |      Gatsb        |
      |      Gatsby       |