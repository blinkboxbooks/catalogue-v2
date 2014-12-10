module KnowsAboutBooks
  def get_book_by_identifier(id)
    http_get :api, "/books/#{id}"
  end

  def get_books_by_identifiers(ids = [])
    raise "Test error: no book ids specified" if ids.empty?
    set_query_param("id", ids)
    http_get :api, "/books"
  end

  def get_book_synopsis(book)
    http_get :api, "/books/#{book["id"]}/synopsis"
  end

  def get_books_by_category(category, filters = {})
    set_query_param("category", category["id"])
    filters.each { |key, value| set_query_param(key, value) } unless filters.empty?
    http_get :api, "/books"
  end

  def get_books_by_contributor(contributor, filters = {})
    set_query_param("contributor", contributor["id"])
    filters.each { |key, value| set_query_param(key, value) } unless filters.empty?
    http_get :api, "/books"
  end

  def get_books_by_promotion(promotion, filters = {})
    set_query_param("promotion", promotion["id"])
    filters.each { |key, value| set_query_param(key, value) } unless filters.empty?
    http_get :api, "/books"
  end

  def get_books_by_publisher(publisher, filters = {})
    set_query_param("publisher", publisher["id"])
    filters.each { |key, value| set_query_param(key, value) } unless filters.empty?
    http_get :api, "books"
  end

  def get_related_books_for_book(book)
    http_get :api, "/books/#{book["id"]}/related"
  end
end

World(KnowsAboutBooks)