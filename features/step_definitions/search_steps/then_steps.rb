Then(/^the search response is a list containing (at least|only) (#{CAPTURE_INTEGER}) (?:books?|suggestions?)$/) do |minimum, count|
  if minimum == "at least"
    validate_book_search_results(@response, min_count: count)
  else
    validate_book_search_results(@response, count: count)
  end
end

And(/^the response has the following attributes:$/) do |table|
  table.hashes.each do |row|
    validate_attribute(@response, row['attribute'], type: row['type'])
  end
end

And(/^the search results display the following attributes for each book:$/) do |table|
  @response["books"].each do |book|
    table.hashes.each do |row|
      validate_attribute(book, row['attribute'], type: row['type'])
    end
  end
end

Then(/^the search response is a list containing (#{CAPTURE_INTEGER}) book(?:s?)$/) do |count|
  validate_book_search_results(@response, count: count)
end

Then(/^the search response is a list containing (#{CAPTURE_INTEGER}) book(?:s?) at offset (#{CAPTURE_INTEGER})$/) do |count, offset|
  validate_book_search_results(@response, count: count, offset: offset)
end

Then(/^the search response is a list containing at least (#{CAPTURE_INTEGER}) book search results ordered by (.*) (.*)$/) do |min_count, direction, ordered_by|
  # Unable to automatically validate #{direction + " " + ordered_by} sort order
  validate_book_search_results(@response, min_count: min_count)
end

Then(/^the search response contains a link to (previous and )?more results$/) do |previous|
    validate_paging_links(@response, previous_link = previous)
end

Then(/^the search response does not contain a link to more results$/) do
  expect(@response["links"].select {|link| link["rel"] == "next"}).to be_empty
end

And(/^all books authors contains "([^"]*)" or "([^"]*)"$/) do |arg1, arg2|
  @response["books"].each do |book|
    matching_authors = book["authors"].select do |author|
      author.include?(arg1) || author.include?(arg2)
    end if book["authors"]
    expect(matching_authors).not_to be_nil if book["authors"]
  end
end

And(/^the first book's (title|author) is "([^"]*)"$/) do |type, term|
  if type == "title"
    expect(@response["books"].first["title"]).to eq(term) 
  elsif type == "author"
    expect(@response["books"].first["authors"]).to include(term)
  end
end

And(/^multiple author names are returned$/) do
  expect(@response["books"].first["authors"].size).to be > 1
end

And(/^each book's content contains "([^"]*)"$/) do |expected_contents|
  @response["books"].each do |book|
    book_text = ""
    book["authors"].each { |author| book_text << author.downcase << " " } unless book["authors"].nil?
    book_text << book["title"].downcase
    expect(book_text).to include(expected_contents)
  end
end

And(/^all the returned books are the same$/) do
  @responses.each do |resp|
    expect(resp["books"]).to eq(@response["books"])
  end
end

And(/^the first book's content contains "([^"]*)"$/) do |expected_contents|
  book = @response["books"].first
  book_text = ""
  book["authors"].each { |author| book_text << author.downcase << " " }
  book_text << book["title"].downcase
  expected_contents.split(" ").each do |expected_word|
    expect(book_text).to include(expected_word.downcase)
  end
end

Then(/^the search response is a list containing (#{CAPTURE_INTEGER}) search suggestions$/) do |count|
  expect(@response["items"].size).to eq(count)
end

And(/^each suggestion contains "([^"]*)"$/) do |content|
  validate_suggestion_search_results(@response, expected_content: content)
end

Then(/^the first book's author is the same as the original book$/) do
  author = subject(:book)["authors"].first
  expect(@response["books"].first["authors"]).to include(author)
end

Then(/^the response contains spelling suggestions$/) do
  expect(@response["suggestions"]).not_to be_empty
end

Then(/^the response contains no spelling suggestions$/) do
  expect(@response["suggestions"]).to be_nil
end

And(/^the suggestion is more than one word$/) do
  expect(@response["suggestions"].first.split(" ").size).to be > 1
end

And(/^the suggestion is "([^"]*)"$/) do |expected_suggestion|
  expect(@response["suggestions"].first).to eq(expected_suggestion)
end
