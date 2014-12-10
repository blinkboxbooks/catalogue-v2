@catalogue @books @data_dependent
Feature: Get a book by identifier
  As an API consumer
  I want to be able to get a book by identifier
  So that I can display the information for a specific book

  @smoke
  Scenario: Getting a book by identifier
    Given there is a book which is currently available for purchase
    When I request that book by identifier
    Then the response is a book
    And the identifier is the one specified
    And it has the following attributes:
      | attribute        | type    | description                     |
      | Title            | String  | The title of the book           |
      | Publication Date | Date    | The date the book was published |
      | Sample Eligible  | Boolean | Whether a sample is available   |
    And it has the following images:
      | relationship | description                 |
      | Cover        | The cover image of the book |
    And it has the following links:
      | relationship    | min | max | description                                                  |
      | Book Price List | 1   | 1   | A list of prices for the book                                |
      | Contributor     | 0   | ∞   | The contributors to the book (i.e. authors, narrators, etc.) |
      | Publisher       | 1   | 1   | The publisher of the book                                    |
      | Sample Media    | 1   | 1   | A sample ePub containing some of the book content            |
      | Synopsis        | 1   | 1   | The synopsis of the book                                     |
  
  @smoke
  Scenario: Books retrieved by identifier are publicly cacheable
    Given there is a book which is currently available for purchase
    When I request that book by identifier
    Then the response is publicly cacheable

  Scenario: Trying to get a book with a nonexistent identifier
    When I request a book by identifier, which does not exist
    Then the request fails because the book was not found

  Scenario: Trying to get a book with a non-alphanumeric identifier
    When I request a book by invalid identifier
    Then the request fails because the book was not found
