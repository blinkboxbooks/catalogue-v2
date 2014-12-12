Given(/^there (?:is|are) at least (?:\w+) book(?:s?) (?:which|whose) (.+)$/) do |condition|
  subject(:book, data_for_a(:book, which: condition))
end

And(/^I want a maximum of (#{CAPTURE_INTEGER}) result(?:s?)$/) do |max_count|
  set_query_param("count", max_count)
end

And(/^I want a maximum of (#{CAPTURE_INTEGER}) results at offset (#{CAPTURE_INTEGER})$/) do |max_count, expected_offset|
  set_query_param("count", max_count)
  set_query_param("offset", expected_offset)
end

And(/^I have specified a sort order of (.*), (.*)$/) do |ordered_by, direction|
  desc = (direction == "descending") ? false : true
  @params = { :order => ordered_by.snake_case.upcase, :desc => desc }
end

And(/^I have not specified a sort order$/) do
end

Given(/^the search query is "([^"]*)"$/) do |term|
  @search_query = term
end

Given(/^the search queries are$/) do |table|
  @search_query = table.raw.flatten.first
  @responses = []
  table.raw.flatten.each { |term| @responses.push(search_for_books(term)) }
end

Given(/^I have entered the query "([^"]*)"$/) do |term|
  @search_query = term.to_s
end

Given(/^the search query is invalid$/) do
  subject(:search_term, data_for_a(:search_term, which: "is invalid"))
end

And(/^(?:the|a) search query (?:that )?will return many results$/) do
  subject(:search_term, data_for_a(:search_term, which: "will return many results"))
  @search_query = subject(:search_term)["query"]
end

Given(/^the search query is one word that is misspelt$/) do
  subject(:search_term, data_for_a(:search_term, which: "is one word that is misspelt"))
  @search_query = subject(:search_term)["query"]
end

Given(/^the search query is multiple words of which one word is misspelt$/) do
  subject(:search_term, data_for_a(:search_term, which: "is multiple words of which one is misspelt"))
  @search_query = subject(:search_term)["query"]
end

Given(/^an invalid isbn "([^"]*)"$/) do |bad_data|
  @search_query = bad_data
end
