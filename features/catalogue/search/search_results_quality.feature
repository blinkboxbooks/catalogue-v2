@data_dependent
@search_quality
@search

Feature: Search results quality
  As an application
  I want to be able to return list of books
  So that users can find related books

  Scenario: Search for books with valid author name in different cases
    Given the search queries are
      | charles dickens |
      | charles Dickens |
      | CHARLES DICKENS |
      | Charles dickens |
      | cHaRleS DicKenS |
    When I search for the query
    Then the search response is a list containing at least ten books
    And all the returned books are the same

  Scenario Outline: Search for books with valid author name in different order
    Given the search query is "<author_name>"
    And I want a maximum of ten results
    When I search for the query
    Then the search response is a list containing at least ten books
    And all books authors contains "david" or "ovason"
  Examples:
    | author_name   |
    | david ovason  |
    | ovason david  |

  Scenario Outline: Search for books with valid author name containing special characters
    Given the search query is "<author_name>"
    When I search for the query
    Then the search response is a list containing at least ten books
    And the first book's author is "<author_name>"
  Examples:
    | author_name   |
    | John le Carré |
    | Darragh Ó Sé  |
    | Günter Eich   |

  Scenario Outline: Search for books with valid title in different cases
    Given the search query is "<book_title>"
    When I search for the query
    Then the search response is a list containing at least ten books
    And the first book's title is "Great Expectations"
  Examples:
    | book_title         |
    | Great Expectations |
    | great expectations |
    | GREAT EXPECTATIONS |
    | gREaT ExPEcTAtIOnS |
    | GREAT expectations |
    | great EXPECTATIONS |

  Scenario Outline: Search for books with valid title containing special characters
    Given the search query is "<book_title>"
    When I search for the query
    Then the search response is a list containing at least ten books
    And the first book's title is "<book_title>"
  Examples:
    | book_title                                   |
    | Eats, Shoots and Leaves                      |
    | The Myth of Wu Tao-tzu                       |
    | Naïve Super                                  |
    | Neil Flambé and the Aztec Abduction          |

  Scenario Outline: Searching for free books with different cases
    Given the search query is "<search_word>"
    When I search for the query
    Then the search response is a list containing at least ten books
  Examples:
    | search_word  |
    | free         |
    | FREE books   |
    | Free eBooks  |
    | Free-Books   |
    | Free e books |
    | Free e-books |

  Scenario Outline: Search for books with valid title and author name
    Given the search query is "<search_word>"
    When I search for the query
    Then the search response is a list containing at least ten books
    And the first book's content contains "<search_word>"
  Examples:
    | search_word                   |
    | Inferno Dan Brown             |
    | Expectations Charles Dickens  |

  Scenario Outline: Search for books with valid author name and title
    Given the search query is "<search_word>"
    When I search for the query
    Then the search response is a list containing at least ten books
    And the first book's content contains "<search_word>"
  Examples:
    | search_word                   |
    | Dan Brown Inferno             |
    | Charles Dickens Expectations  |

  Scenario Outline: Search for books with fragmented author name and title
    Given the search query is "<search_query>"
    When I search for the query
    Then the search response is a list containing at least ten books
    And the first book's title is "<book_title>"
    And the first book's author is "<author_name>"
  Examples:
    | search_query          | book_title              | author_name     |
    | hawking history time  | A Brief History Of Time | Stephen Hawking |

  Scenario Outline: Search for books with fragmented title and author name
    Given the search query is "<search_query>"
    When I search for the query
    Then the search response is a list containing at least ten books
    And the first book's title is "<book_title>"
    And the first book's author is "<author_name>"
  Examples:
    | search_query  | author_name | book_title  |
    | inferno brown | Dan Brown   | Inferno     |
