@search @data_dependent
Feature: The results returned from the search service are well formatted
  As an API user
  I want search results to contain well formed authors and books
  So that I can display these results to the customer

  @smoke
  Scenario: Searches include book results
    Given there is at least one book which can be searched for
    When I perform a search for that book
    Then the search response is a list containing at least one book
    And the response has the following attributes:
      | attribute       | type    | description                    |
      | numberOfResults | Integer | Number of search results found |
    And the search results display the following attributes for each book:
      | attribute | type   | description                             |
      | title     | String | The title of the book                   |
      | id        | String | The ISBN of the book                    |

  @smoke
  Scenario: Empty searches raise an error
    When I perform a search without a query
    Then the request fails because it is invalid

  Scenario: Listing a maximum number of search results
    Given there are at least four books which can be searched for
    And I want a maximum of two results
    When I perform a search for a book
    Then the search response is a list containing two books
    
  Scenario: Listing a maximum number of search results, at an offset
    Given there are at least six books which can be searched for
    And I want a maximum of three results at offset two
    When I perform a search for a book
    Then the search response is a list containing three books at offset two

  Scenario: Listing search results with default offset
    Given there are at least two books which can be searched for
    And I want a maximum of one result
    When I perform a search for a book
    Then the search response is a list containing one book at offset zero

  Scenario: Search results contain links to the next page of results
    Given a search query that will return many results
    When I search for the query
    Then the search response contains a link to more results

  Scenario: Paginated search results contain a link to the previous page of results
    Given a search query that will return many results
    And I want a maximum of 10 results at offset one
    When I search for the query
    Then the search response contains a link to previous and more results

  Scenario: Search responses are cacheable
    Given there is at least one book which can be searched for
    When I perform a search for that book
    Then the search response is a list containing at least one book

  # We are unable to automatically verify sorting as the information is not returned by the service.
  @manual
  Scenario Outline: Search sort order
    Given there are at least two books which can be searched for
    And I have specified a sort order of <ordered_by>, <direction>
    When I perform a search for a book
    Then the search response is a list containing at least two book search results ordered by <direction> <ordered_by>

    Examples: Metadata available on request
      | ordered_by       | direction  |
      | author           | ascending  |
      | author           | descending |
      | relevance        | ascending  |
      | relevance        | descending |
      | popularity       | ascending  |
      | popularity       | descending |
      | price            | ascending  |
      | price            | descending |
      | publication date | ascending  |
      | publication date | descending |

  Scenario: Default search sort order is descending relevance
    Given there are at least two books which can be searched for
    And I have not specified a sort order
    When I perform a search for a book
    Then the search response is a list containing at least two book search results ordered by descending relevance
