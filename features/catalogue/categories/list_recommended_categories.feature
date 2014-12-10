@categories @list_categories
Feature: List recommended categories
  As an API consumer
  I want to be able to list recommended categories of any kind
  So that they can be shown to users in a prominent position

  @smoke @wip
  Scenario: Listing recommended categories
    Given there is a category which is recommended
    When I request the recommended categories
    Then the response is a list containing at least that one category
    And each category's recommended sequence is in ascending order

  @smoke
  Scenario: Listing categories that are not recommended
    Given there is a category which is not recommended
    When I request the non-recommended categories
    Then the response is a list containing at least that one category
    And each category's sequence is in ascending order