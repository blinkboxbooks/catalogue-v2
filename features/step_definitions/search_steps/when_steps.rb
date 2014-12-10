When(/^I perform a search for (?:a|that) book$/) do
  search_for_books(subject(:book)['title'], parse: true)
end

When(/^I perform a search without a query$/) do
  search_for_books("", parse: false)
end

When(/^I search for the query$/) do
  search_for_books(@search_query, parse: true)
end

When(/^I search for the invalid query$/) do
  search_for_books(@search_query, parse: false)
end

When(/^I request search suggestions for the query$/) do
  search_for_suggestions(@search_query, parse: true)
end

When(/^I request similar books$/) do
  search_for_similar(subject(:book)['id'], parse: true)
end

When(/^I search for the book by its isbn$/) do
  search_for_books(subject(:book)['id'], parse: true)
end

When(/^I request similar books for the invalid isbn$/) do
  search_for_similar(@search_query, parse: false)
end
