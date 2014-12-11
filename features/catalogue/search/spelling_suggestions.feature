@search
Feature: Spelling Suggestions
  As an application
  I want to suggest spelling corrections to users searches
  So they can find what they are searching for

  @smoke
  Scenario: Suggest a spelling correction for a misspelt one word search query
    Given the search query is one word that is misspelt
    When I search for the query
    Then the response contains one spelling suggestion

  Scenario Outline: Suggest a spelling correction for multi-word search query of which one is misspelt
    Given the search query is "<misspelt_multiword_query>"
    When I search for the query
    Then the response contains one spelling suggestion
    And the suggestion is "<correctly_spelled_query>"
    And it is publicly cacheable

  Examples:
    | misspelt_multiword_query | correctly_spelled_query |
    | tiem space               | time space              |

  Scenario: Do not suggest spelling for correctly spelled queries
    Given there is at least one book which can be searched for
    When I perform a search for that book
    Then the response contains no spelling suggestions
