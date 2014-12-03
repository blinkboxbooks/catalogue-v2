module KnowsAboutPrices
  def get_prices_for_books(books)
    if books.kind_of?(Array)
      ids = books.map { |book| book["id"] }
    else
      ids = books["id"]
    end
    set_query_param("book", ids)
    http_get :api, "/prices"
  end
end

World(KnowsAboutPrices)