@categories @list_categories
Feature: Find categories by location
  As an API consumer
  I want to be able to list categories by location
  So that they can be displayed in prominent spaces

  @smoke
  Scenario: Listing categories by location
    Given there is a category which is used on the front page
    When I request the category, by location
    Then the response is a list containing one category
    And its location is the one I requested

  @negative @CP-1242
  Scenario: Listing categories by nonexistent location
    When I request the category, by location which does not exist
    # uncomment once fixed
    #Then the request fails because it is invalid