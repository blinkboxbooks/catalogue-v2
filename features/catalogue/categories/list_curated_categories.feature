@categories @list_categories
Feature: List curated categories
  As an API consumer
  I want to be able to list curated categories
  So that they can be displayed in prominent spaces on key screens

  @smoke @wip
  Scenario: Listing curated categories
    Given there is a category which is curated
    When I request the curated categories
    Then the response is a list containing at least one category
    And each category's sequence is in ascending order