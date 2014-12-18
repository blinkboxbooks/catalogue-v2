@search
Feature: Spelling Suggestions
  As an application
  I want to suggest spelling corrections to users searches
  So they can find what they are searching for

  @smoke
  Scenario: Suggest a spelling correction for a misspelt one word search query
    Given the search query is one word that is misspelt
    When I search for the query
    Then the response contains spelling suggestions

  Scenario Outline: Suggest a spelling correction for multi-word search queries
    Given the search query is "<misspelt_multiword_query>"
    When I search for the query
    Then the response contains spelling suggestions
    And the suggestion is "<correctly_spelled_query>"
    And it is publicly cacheable

  Examples:
    | misspelt_multiword_query    | correctly_spelled_query     |
    | tiem space                  | time space                  |
    | tiem unaverse               | tiem universe               |
    | cat unaverse octobir        | cat universe october        |
    | tiem unaverse octobir       | time universe october       |
    | tiem unaverse octobir quikc | time universe october quick |

  Scenario: Do not suggest spelling for correctly spelled queries
    Given there is at least one book which can be searched for
    When I perform a search for that book
    Then the response contains no spelling suggestions
