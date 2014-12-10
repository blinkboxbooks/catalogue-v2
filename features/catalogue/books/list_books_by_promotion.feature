@catalogue @books @promotions @data_dependent
Feature: List books by promotion
  As an API consumer
  I want to be able to list books within a promotion
  So that I can view promoted books

  @smoke @CP-1241
  Scenario: Listing books
    Given there is a promotion which is currently active
    When I request the books, by that promotion
    Then the response is a list containing at least eight books

  @smoke
  Scenario: Lists of books by promotion are publicly cacheable
    Given there is a promotion which is currently active
    When I request the books, by that promotion
    Then the response is publicly cacheable

  @CP-1241 @CP-1242
  Scenario: Listing books for a nonexistent promotion
    When I request the books, by a promotion which does not exist
    Then the response is a list containing no books

  # added a @pending tag as the primary sort orders are checked, but at the moment secondary
  # and tertiary sort orders are not. this should be added to the validate_list_order function
  @pending @CP-1241
  Scenario Outline: Listing books in specified orders
    Given there is a promotion which is currently active
    And I have specified a sort order of <order>, <direction>
    When I request the books, by that promotion
    Then the response is a list containing at least eight books in the specified order

    Examples:
      | order            | direction  | secondary order  | secondary direction |
      | Title            | ascending  | Publication Date | descending          |
      | Title            | descending | Publication Date | descending          |
      | Publication Date | ascending  | Title            | ascending           |
      | Publication Date | descending | Title            | ascending           |

  # these sort orders don't have an easy way to check whether they're right, as there's no sales rank
  # attribute and price is a separate entity. for now we'll just check that the sort orders work but
  # in future we should probably do something better and actually check their correctness.
  @pending @CP-1241
  Scenario Outline: Listing books in specified orders
    Given there is a promotion which is currently active
    And I have specified a sort order of <order>, <direction>
    When I request the books, by that promotion
    Then the response is a list containing at least eight books

    Examples:
      | order      | direction  | secondary order  | secondary direction | tertiary order | tertiary direction |
      | Price      | ascending  | Publication Date | descending          | Title          | ascending          |
      | Price      | descending | Publication Date | descending          | Title          | ascending          |
      | Sales Rank | ascending  | Publication Date | descending          | Title          | ascending          |
      | Sales Rank | descending | Publication Date | descending          | Title          | ascending          |
      | Sequential | ascending  | Publication Date | descending          | Title          | ascending          |
      | Sequential | descending | Publication Date | descending          | Title          | ascending          |

  Scenario: Trying to list books with an invalid sort order
    Given there is a promotion which is currently active
    And I have specified a sort order of something invalid, ascending
    When I request the books, by that promotion
    Then the request fails because it is invalid

  Scenario: Trying to list books with an invalid sort direction
    Given there is a promotion which is currently active
    And I have specified a sort order of title, invalid
    When I request the books, by that promotion
    Then the request fails because it is invalid

  Scenario: Listing a maximum number of books
    Given there is a promotion which is currently active
    When I request the books, by that promotion, using the following filters:
      | count     | 3       |
    Then the response is a list containing three books

  Scenario: Listing a maximum number of books, at an offset
    Given there is a promotion which is currently active
    When I request the books, by that promotion, using the following filters:
      | count     | 3       |
      | offset    | 5       |
    Then the response is a list containing 3 books at offset 5

  Scenario: Trying to list books with a negative count
    Given there is a promotion which is currently active
    When I request the books, by that promotion, using the following filters:
      | count     | -1      |
    Then the request fails because it is invalid

  Scenario: Trying to list books with a negative offset
    Given there is a promotion which is currently active
    When I request the books, by that promotion, using the following filters:
      | count     | 10      |
      | offset    | -1      |
    Then the request fails because it is invalid

  @CP-1241
  Scenario: Listing books filtered by a valid publication date range
    Given there is a promotion which is currently active
    When I request the books, by that promotion, using the following filters:
      | min publication date | 2010-10-01 |
      | max publication date | 2014-10-01 |
    Then the response is a list containing at least three books

  Scenario: Trying to list books filtered by an invalid publication date range
    Given there is a promotion which is currently active
    When I request the books, by that promotion, using the following filters:
      | min publication date | 2013-01-01 |
      | max publication date | 2012-01-01 |
    Then the request fails because it is invalid
