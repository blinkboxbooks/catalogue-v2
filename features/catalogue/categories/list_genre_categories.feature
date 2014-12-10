@categories @list_categories
Feature: List genre categories
  As an API consumer
  I want to be able to list genre categories
  So that I can allow users to choose a genre they are interested in

  @smoke
  Scenario: Listing genre categories
    Given there is a category which is a genre category
    When I request the genre categories
    Then the response is a list containing at least ten categories
    And each category's sequence is in ascending order

  Scenario: Listing categories of nonexistent kind
    When I request the categories of nonexistent kind
    Then the request fails because it is invalid