@catalogue @books @synopsis @data_dependent
Feature: Get the synopsis for a book
  As an API consumer
  I want to be able to get the synopsis for a book
  So that the I am able to see what the book is about

  @smoke
  Scenario: Getting the synopsis for a book
    Given there is a book which is currently available for purchase
    When I request the synopsis for it
    Then the response is a synopsis
    And it has the following attributes:
      | attribute | type    | description       |
      | Text      | String  | The synopsis text |

  @smoke
  Scenario: Books synopses are publicly cacheable
    Given there is a book which is currently available for purchase
    When I request the synopsis for it
    Then the response is publicly cacheable

  Scenario: Getting the synopsis for a book with a nonexistent identifier
    Given there is a book which does not exist
    When I request the synopsis for it
    Then the request fails because the book was not found