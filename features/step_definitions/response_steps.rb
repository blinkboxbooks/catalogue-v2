# the capture group is a fairly horrible hack to match any string that doesn't contain the word
# 'list' anywhere in it. this is necessary because cucumber only works with regex, and regex
# doesn't really do negative matches very well. if you really need to edit it then have a look at
# zero-width negative lookaheads and non-capturing groups.
Then(/^the response is a (newly created )?((?:(?!list).)+)$/) do |created, item_type|
  if created
    Cucumber::Rest::Status.ensure_status(201)
    expect(HttpCapture::RESPONSES.last["Location"]).to_not be_nil, "Create responses must have a Location header"
  else
    Cucumber::Rest::Status.ensure_status(200)
  end
  data = parse_response_data
  send("validate_#{item_type.snake_case}", data)
end

Then(/^the response is a(?: (.+))? list containing no (?:.+)s$/) do |list_type|
  Cucumber::Rest::Status.ensure_status(200)
  data = parse_response_data
  validate_list(data, list_type: list_type, min_count: 0, max_count: 0,  warn_only: %i{count number_of_results offset empty_items})
end

Then(/^the response is a(?: (.+))? list containing (#{CAPTURE_INTEGER}) (.+?s?)(?: at offset (#{CAPTURE_INTEGER}))?( in the (specified|.+?) order)?$/) do |list_type, count, item_type, offset, ordered, order_field|
  Cucumber::Rest::Status.ensure_status(200)
  data = parse_response_data
  validate_list(data, list_type: list_type, item_type: item_type.singularize.snake_case, count: count, offset: offset, warn_only: %i{count number_of_results offset})
  query["order"] = order_field unless order_field == "specified"
  validate_list_order(data, query["order"].camel_case, descending: query["desc"]) if ordered
end

Then(/^the response is a(?: (.+))? list containing at least (#{CAPTURE_INTEGER}) (.+?s?)(?: at offset (#{CAPTURE_INTEGER}))?( in the specified order)?$/) do |list_type, count, item_type, offset, ordered|
  Cucumber::Rest::Status.ensure_status(200)
  data = parse_response_data
  validate_list(data, list_type: list_type, item_type: item_type.singularize.snake_case, min_count: count, offset: offset, warn_only: %i{count number_of_results empty_items})
  validate_list_order(data, query["order"].camel_case, descending: query["desc"]) if ordered
end

Then(/^the response is a list containing at least that one (category|publisher)$/) do |subject_name|
  Cucumber::Rest::Status.ensure_status(200)
  parse_response_data
  items = @response_data["items"].select { |item| item["id"] == subject(subject_name.to_sym)["id"] }
  expect(items).not_to be_empty
end

Then(/^the identifiers are of existent (books|contributors|publishers)$/) do |subject_name|
  expected_ids = subject(subject_name.to_sym).map { |subject| subject["id"]}
  actual_ids = @response_data["items"].map { |item| item["id"] }
  expect(actual_ids).to match_array(expected_ids)
end

Then(/^the identifiers are present (and in order, )?as specified$/) do |ordered|
  actual_ids = @response_data["items"].map { |item| item["id"] }
  if ordered
    expect(actual_ids).to eq(subject(:ids))
  else
    expect(actual_ids).to match_array(subject(:ids))
  end
end

# Lets you specify the attributes of an entity in the Gherkin. This is only really intended for use on one test per
# entity type (probably a "Get X by identifier" test) with the purpose of hauling key attributes into the Gherkin
# specification rather than the more common approach of having them in the validate_X function which allows reusability
# throughout many tests. Note that things like id, guid, etc. shouldn't be listed here - we're just interested in the
# business-level requirements not the implementation details.
Then(/^it has the following attributes:$/) do |table|
  table.hashes.each do |row|
    attribute_name = row["attribute"].camel_case
    expected_value = row["value"] if row["value"] && row["value"].length > 0
    validate_attribute(@response_data, attribute_name, type: row["type"], value: expected_value)
  end
end

Then(/^each (.+) has the following attributes:$/) do |subject_name, table|
  @response_data["items"].each do |item|
    table.hashes.each do |row|
      attribute_name = row["attribute"].camel_case
     if row["value"] && row["value"].length > 0
       expected_value = row["value"]
     else
       expected_value = subject(subject_name.snake_case.to_sym)[attribute_name]
     end
      validate_attribute(item, attribute_name, type: row["type"], value: expected_value)
    end
  end
end

Then(/^it has the following images:$/) do |table|
  rels = table.hashes.map { |row| row["relationship"] }
  validate_images(@response_data, *rels)
end

Then(/^each (?:.+) has the following images:$/) do |table|
  rels = table.hashes.map { |row| row["relationship"] }
  @response_data["items"].each do |item|
    validate_images(item, *rels)
  end
end

Then(/^it has the following links:$/) do |table|
  validate_links(@response_data, *table.hashes)
end

Then(/^each (?:.+) has the following links:$/) do |table|
  @response_data["items"].each do |item|
    validate_links(item, *table.hashes)
  end
end

Then(/^the identifier is the one specified$/) do
  expect(@response_data["id"]).to eq subject(:id)
end

Then(/^that category has a correct (.+)$/) do |key|
  attribute_name = key.camel_case
  item = @response_data["items"][0]
  expect(item[attribute_name]).to eq subject(:category)[attribute_name]
end

Then(/^its location is the one I requested$/) do
  actual_category = @response_data["items"][0]
  expect(actual_category["location"]).to eq subject(:category)["location"]
end

# And each category's recommended sequence number is in ascending order
Then(/^each (?:.+)'s (.+) is in (ascending|descending) order$/) do |key, direction|
  attribute_name = key.camel_case
  attribute_values = @response_data["items"].map.with_index do |item, index|
    value = item[attribute_name]
    expect(value).to_not be_nil, "'#{attribute_name}' is nil in item at index #{index}"
  end
  attribute_values.map! { |v| DateTime.parse(v) } if key =~ /date$/i

  expected_values = attribute_values.sort
  expected_values.reverse! if direction == "descending"
  expect(attribute_values).to eq(expected_values)
end

Then(/^the book price has a discount price also$/) do
  actual_price = @response_data["items"][0]
  expect(actual_price["discountPrice"]).to be >= 0
end
