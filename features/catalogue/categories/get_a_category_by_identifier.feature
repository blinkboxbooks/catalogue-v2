@categories @get_category
Feature: Get a category by identifier
  As an API consumer
  I want to be able to get a category by identifier
  So that I can display the information for a specific category

  @smoke @data_dependent
  Scenario: Getting a category by identifier
    Given there is a category which has been curated
    When I request that category by identifier
    Then the response is a category
    And the identifier is the one specified
    And it has the following attributes:
      | attribute    | type    | description                                 |
      | slug         | String  | A URL-safe name that can be used for links  |
      | display name | String  | The display name                            |
      | sequence     | Integer | The curated sequence number of the category |

  @smoke @data_dependent
  Scenario: Categories retrieved by identifier are publicly cacheable
    Given there is a category which is currently active
    When I request that category by identifier
    Then the response is publicly cacheable

  @negative
  Scenario: Trying to get a category with a nonexistent identifier
    When I request a category by identifier, which does not exist
    Then the request fails because the category was not found
