@categories @find_categories
Feature: Find categories by slug
  As an API consumer
  I want to be able to find categories by slug
  So that I can use the slug in customer-friendly URLs

  @smoke @data_dependent @CP-1241
  Scenario: Getting a category by slug
    Given there is a category which is currently active
    When I request that category, by slug
    Then the response is a list containing one category
    And that category has a correct display name

  @CP-1241 @CP-1242
  Scenario: Trying to get a category with a nonexistent slug
    When I request the category, by slug which does not exist
    Then the response is a list containing no categories

  Scenario: Listing categories by slug is publicly cacheable
    Given there is a category which is currently active
    When I request that category, by slug
    Then the response is publicly cacheable