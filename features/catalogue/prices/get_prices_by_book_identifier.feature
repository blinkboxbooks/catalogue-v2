@catalogue @books @prices @data_dependent
Feature: Get prices by book identifier
  As an API consumer
  I want to be able to get the prices and Clubcard points for specific books
  So that customers know how much the books cost and how many points will be awarded

  @smoke @CP-1243
  Scenario Outline: Getting the prices and points for a book
    When I request the price for a book identified by "<isbn>"
    Then the response is a list containing one book price
    And each book price has the following attributes:
      | attribute             | type    | value    | description                                       |
      | currency              | String  | GBP      | The ISO 4217 three-letter currency code           |
      | price                 | Float   | <price>  | The price of the book in the specified currency   |
      | clubcard points award | Integer | <points> | The number of Clubcard points awarded on purchase |

  Examples: A selection of books at various prices
    | isbn          | price  | points |
    | 9780062331731 | 0.99   | 0      |
    | 9781907652950 | 1.00   | 1      |
    | 9781476727097 | 1.99   | 1      |
    | 9780007578276 | 2.00   | 2      |
    | 9781409049548 | 4.49   | 4      |
    | 9781135091477 | 120.00 | 120    |

  @smoke @CP-1243
  Scenario: Book prices have a link to the book they are the price for
    Given there is a book which is currently available for purchase
    When I request the price for that book
    Then the response is a list containing one book price
    And each book price has the following links:
      | relationship | min | max | description                    |
      | Book         | 1   | 1   | The book that the price is for |

  @smoke
  Scenario: Book price lists are publicly cacheable
    Given there is a book which is currently available for purchase
    When I request the price for that book
    Then the response is publicly cacheable

  @smoke @CP-1243
  Scenario: Getting the price for a free book returns a 0.0 price
    Given there is a book which is free
    When I request the price for that book
    Then the response is a list containing one book price
    And each book price has the following attributes:
      | attribute             | type    | value    | description                                       |
      | currency              | String  | GBP      | The ISO 4217 three-letter currency code           |
      | price                 | Float   | 0.00     | The price of the book in the specified currency   |
      | clubcard points award | Integer | 0        | The number of Clubcard points awarded on purchase |

  @CP-1243
  Scenario: Getting the prices for a nonexistent book returns no prices
    When I request the price for a book which does not exist
    Then the response is a list containing no book prices

  Scenario: Getting prices for multiple books returns multiple prices in requested order
    Given there are three books, each of which is currently available for purchase
    When I request the prices for these books
    Then the response is a list containing three book prices
    And the identifiers are present and in order, as specified

  @CP-1243
  Scenario: Getting prices for multiple books where some are nonexistent returns prices for found books
    Given there are two books, each of which is currently available for purchase
    When I request the prices for these books, and a book which does not exist
    Then the response is a list containing two book prices

  @CP-1816
  Scenario: Getting prices for 100 books
    When I request the price for 100 books
    Then the response is a list containing 100 book prices

  @CP-1816 @negative
  Scenario: Getting prices for 101 books fails
    When I request the price for 101 books
    Then the request fails because it is invalid

  @CP-2009
  Scenario: Getting the price for a book with a discount
    Given there is a book which has a discount
    When I request the price for that book
    Then the response is a list containing one book price
    And the book price has a discount price also
