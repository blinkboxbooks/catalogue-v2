@catalogue @books @data_dependent
Feature: List related books for a book
  As an API consumer
  I want to be able to list the related books for a book
  So that the user is able to find similar content they may be interested in

  @smoke @CP-1241
  Scenario: Listing the related books for a book
    Given there is a book which has related books
    When I request the related books for that book
    Then the response is a list containing at least one book

  @smoke
  Scenario: Lists of related books for a book are publicly cacheable
    Given there is a book which is currently available for purchase
    When I request the related books for that book
    And it is publicly cacheable

  @CP-1237
  Scenario: Getting the related books for a book with a nonexistent identifier
    When I request the related books for a book which does not exist
    # Uncomment once fixed
    #Then the request fails because the book was not found
