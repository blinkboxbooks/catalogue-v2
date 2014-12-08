Given(/^I have specified a sort order of (.+), (.+)/) do |order, direction|
  set_query_param("order", order)
  set_query_param("desc", case direction
                          when "descending" then true
                          when "ascending" then false
                          else direction
                          end)
end

Given(/^there is a (book|category|contributor|contributor group|promotion|publisher) which (.+)$/) do |subject_name, condition|
  data = data_for_a(subject_name, which: condition)
  subject(subject_name.snake_case.to_sym, data)
end

Given(/^there are (#{CAPTURE_INTEGER}) (books|contributors|publishers), each of which (.+)$/) do |count, subject_name, condition|
  data = data_for_a(subject_name.singularize, which: condition, instances: count)
  subject(subject_name.to_sym, data)
end

Given(/^there is a book with an interesting price point$/) do
  subject(:book, data_for_a(:book, which: "has an interesting price point"))
  subject(:book_price, subject(:book)["book price"])
end

When(/^I request (?:a|the) (book|contributor|publisher|contributor group) by invalid identifier$/) do |subject_name|
  send("get_#{subject_name.snake_case}_by_identifier", "*")
end

When(/^I request (?:the|these) (books|contributors|publishers) (?:and a (?:book|contributor|publisher) which (.+), )?by identifiers$/) do |subject_name, other_condition|
  ids = subject(subject_name.to_sym).map { |subject| subject["id"] }
  subject(:ids, ids)
  ids << data_for_a(subject_name.singularize, which: other_condition)["id"] if other_condition
  send("get_#{subject_name}_by_identifiers", ids)
end

When(/^I request (?:a|the|that) (book|category|contributor|publisher|contributor group) by identifier(?:, which (.+))?$/) do |subject_name, condition|
  subject(subject_name.snake_case.to_sym, data_for_a(subject_name, which: condition)) if condition
  id = subject(subject_name.snake_case.to_sym)["id"]
  subject(:id, id)
  send("get_#{subject_name.snake_case}_by_identifier", id)
end

When(/^I request (#{CAPTURE_INTEGER}) (books|contributors|publishers) that do not exist, by identifiers$/) do |count, subject_name|
  subjects =  data_for_a(subject_name.singularize.to_sym, which: "does not exist", instances: count)
  ids = subjects.map { |subject| subject["id"] }
  send("get_#{subject_name}_by_identifiers", ids)
end

When(/^I request the books,? (?:by|for) (?:a|that) (category|contributor|promotion|publisher)(?: which (.+))?$/) do |subject_name, condition|
  subject(subject_name.to_sym, data_for_a(subject_name, which: condition)) if condition
  send("get_books_by_#{subject_name}", subject(subject_name.to_sym))
end

When(/^I request the price for a book identified by "(.+)"$/) do |isbn|
  get_prices_for_books("id" => isbn)
end

When(/^I request the price for (#{CAPTURE_INTEGER}) books$/) do |count|
  data = data_for_a("book", which: "is currently available for purchase")
  books = Array.new(count) { data }
  get_prices_for_books(books)
end

When(/^I request the prices? for (?:a|that|these) (books?)(?: which (.+))?$/) do |subject_name, condition|
  subject(subject_name.to_sym, data_for_a(subject_name, which: condition)) if condition
  ids = [subject(subject_name.to_sym)].flatten.map { |subject| subject["id"] + '+GBP' }
  subject(:ids, ids)
  get_prices_for_books(subject(subject_name.to_sym))
end

When(/^I request the prices for these books, and a book which (.+)$/) do |book_condition|
  query_books = subject(:books)
  query_books << data_for_a(:book, which: book_condition)
  get_prices_for_books(query_books)
end

When(/^I request the books, by that (category|contributor|promotion|publisher), using the following filters:$/) do |subject_name, table|
  filters = table.rows_hash
  send("get_books_by_#{subject_name}", subject(subject_name.to_sym), filters)
end

When(/^I request the related books for (?:a|that) book(?: which (.+))?$/) do |book_condition|
  subject(:book, data_for_a(:book, which: book_condition)) if book_condition
  get_related_books_for_book(subject(:book))
end

When(/^I request all (categories|publishers|contributor groups), using the following filters:$/) do |subject_name, table|
  filters = table.rows_hash
  send("get_all_#{subject_name.snake_case}", filters)
end

When(/^I request all (categories|publishers)$/) do |subject_name|
  send("get_all_#{subject_name}")
end

When(/^I request (?:the|that) category, by (location|slug)(?: which (.+))?$/) do |type, condition|
  subject(:category, data_for_a(:category, which: condition)) if condition
  subject(:type, type)
  send("get_category_by_#{type}", subject(:category)[type])
end

When(/^I request the categories of (.+) kind$/) do |kind|
  get_categories_by_kind(kind)
end

When(/^I request the (curated|genre) categories$/) do |kind|
  get_categories_by_kind(kind)
end

When(/^I request the (recommended|non-recommended) categories$/) do |kind|
  kind = kind.tr("-", "_")
  send("get_#{kind}_categories")
end

When(/^I request the contributors, by group name(?: which (.+))?$/) do |condition|
  subject(:contributor_group, data_for_a("contributor group", which: condition)) if condition
  get_contributors_by_group_name(subject(:contributor_group)["name"])
end

When(/^I request the synopsis for it$/) do
  get_book_synopsis(subject(:book))
end
