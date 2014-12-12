module KnowsAboutSearchServiceRequests
  def search_for_books(query, parse: true)
    set_query_param("q", query)
    http_get :search, "/books"
    @response = parse_response_data if parse
  end

  def search_for_suggestions(query, parse: true)
    set_query_param("q", query)
    http_get :search, "/suggestions"
    @response = parse_response_data if parse
  end

  def search_for_similar(isbn, parse: true)
    url = "/books/#{isbn}/similar"
    http_get :search, url
    @response = parse_response_data if parse
  end
end

World(KnowsAboutSearchServiceRequests)
