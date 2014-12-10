module KnowsAboutSearchServiceRequests
  def search_for_books(query, parse: true)
    query = url_encode(query)
    query += "&#{@params.to_query}" if @params
    http_get :search, "/books?q=#{query}"
    @response = parse_response_data if parse
  end

  def search_for_suggestions(query, parse: true)
    http_get :search, "/suggestions?q=#{url_encode(query)}"
    @response = parse_response_data if parse
  end

  def search_for_similar(isbn, parse: true)
    url = "/books/#{isbn}/similar"
    url += "?#{@params.to_query}" if @params
    http_get :search, url
    @response = parse_response_data if parse
  end

  def url_encode(string)
    URI.encode_www_form_component(string)
  end
end

World(KnowsAboutSearchServiceRequests)
