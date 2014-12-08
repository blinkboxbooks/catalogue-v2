@catalogue @books @contributors @data_dependent
Feature: List books by contributor
  As an API consumer
  I want to be able to list books associated to a contributor
  So that I can browse books by contributors I am interested in

  @smoke @CP-1241
  Scenario: Listing books
    Given there is a contributor which is an author
    When I request the books, by that contributor
    Then the response is a list containing at least five books

  @smoke
  Scenario: Lists of books by contributor are publicly cacheable
    Given there is a contributor which is an author
    When I request the books, by that contributor
    Then the response is publicly cacheable

  @CP-1241 @CP-1242
  Scenario: Listing books for a nonexistent contributor
    When I request the books, by a contributor which does not exist
    Then the response is a list containing no books

  # added a @pending tag as the primary sort orders are checked, but at the moment secondary
  # and tertiary sort orders are not. this should be added to the validate_list_order function
  @pending @CP-1241
  Scenario Outline: Listing books in specified orders
    Given there is a contributor which is an author
    And I have specified a sort order of <order>, <direction>
    When I request the books, by that contributor
    Then the response is a list containing at least five books in the specified order

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
    Given there is a contributor which is an author
    And I have specified a sort order of <order>, <direction>
    When I request the books, by that contributor
    Then the response is a list containing at least five books

    Examples:
      | order      | direction  | secondary order  | secondary direction | tertiary order | tertiary direction |
      | Price      | ascending  | Publication Date | descending          | Title          | ascending          |
      | Price      | descending | Publication Date | descending          | Title          | ascending          |
      | Sales Rank | ascending  | Publication Date | descending          | Title          | ascending          |
      | Sales Rank | descending | Publication Date | descending          | Title          | ascending          |
  
  Scenario: Trying to list books with an invalid sort order
    Given there is a contributor which is an author
    And I have specified a sort order of something invalid, ascending
    When I request the books, by that contributor
    Then the request fails because it is invalid

  Scenario: Trying to list books with an invalid sort direction
    Given there is a contributor which is an author
    And I have specified a sort order of title, invalid
    When I request the books, by that contributor
    Then the request fails because it is invalid

  Scenario: Listing a maximum number of books
    Given there is a contributor which is an author
    When I request the books, by that contributor, using the following filters:
      | count     | 3       |
    Then the response is a list containing three books

  Scenario: Listing a maximum number of books, at an offset
    Given there is a contributor which is an author
    When I request the books, by that contributor, using the following filters:
      | count     | 2       |
      | offset    | 3       |
    Then the response is a list containing 2 books at offset 3

  Scenario: Trying to list books with a negative count
    Given there is a contributor which is an author
    When I request the books, by that contributor, using the following filters:
      | count     | -1      |
    Then the request fails because it is invalid

  Scenario: Trying to list books with a negative offset
    Given there is a contributor which is an author
    When I request the books, by that contributor, using the following filters:
      | count     | 10      |
      | offset    | -1      |
    Then the request fails because it is invalid

  @CP-1241
  Scenario: Listing books filtered by a valid publication date range
    Given there is a contributor which is an author
    When I request the books, by that contributor, using the following filters:
      | min publication date | 2009-01-01 |
      | max publication date | 2012-01-01 |
    Then the response is a list containing at least two books

  Scenario: Listing books filtered by a invalid publication date range
    Given there is a contributor which is an author
    When I request the books, by that contributor, using the following filters:
      | min publication date | 2013-01-01 |
      | max publication date | 2011-01-01 |
    Then the request fails because it is invalid