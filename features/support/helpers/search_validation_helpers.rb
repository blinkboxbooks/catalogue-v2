module KnowsAboutSearchServiceResponseValidation
  def validate_book_search_results(data, count: 50, min_count: 1, offset: 0)
    validate_attribute(data, "type", type: String)
    validate_attribute(data, "numberOfResults", type: Numeric)
    validate_attribute(data, "id", type: String) unless data["type"].include?("similar") #the response for search/books/isbn/similar does not return an id
    actual_count = 0
    data['books'].each do |book|
      actual_count += 1
      validate_book_result(book)
    end if data["books"]
    expect(actual_count).to eq(count) if count != 50
    expect(actual_count).to be >= min_count
    validate_offset(data, offset) if offset != 0
  end

  def validate_suggestion_search_results(data, count: 10, expected_content: " ")
    suggestions = data["items"]
    actual_count = 0
    suggestions.each do |suggestion|
      actual_count += 1
      validate_contributor_suggestion(suggestion) if suggestion["type"].include?("contributor")
      validate_book_suggestion(suggestion) if suggestion["type"].include?("book")
      validate_suggestion_contents(suggestion, expected_content)
    end
    expect(actual_count).to eq(count)
  end

  def validate_book_result(data)
    validate_attribute(data, "id", type: String)
    validate_attribute(data, "title", type: String)
    validate_attribute(data, "authors", type: Array) if data["authors"]
  end

  def validate_offset(data, offset)
    data["links"].each do |link|
      expect(link["href"]).to include("offset=#{offset}") if link["rel"] == "this"
    end
  end

  def validate_contributor_suggestion(data)
    validate_attribute(data, "title", type: String)
    validate_attribute(data, "id", type: String)
  end

  def validate_book_suggestion(data)
    validate_attribute(data, "title", type: String)
    validate_attribute(data, "id", type: String)
    validate_attribute(data, "authors", type: Array) if data["authors"]
  end

  def validate_suggestion_contents(data, expected_content)
    text = ""
    text << data["title"] << " "
    data["authors"].each do |author|
      text << author << " "
    end if data["authors"]
    expect(text.downcase).to include(expected_content.downcase)
  end
end

World(KnowsAboutSearchServiceResponseValidation)
