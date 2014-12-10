# encoding: utf-8
module KnowsAboutResponseValidation

  def validate_image(data)
    validate_attribute(data, "rel", type: String) { |value| expect(value).to start_with "urn:blinkboxbooks:image:" }
    validate_attribute(data, "src", type: String) { |value| expect(value).to match(URI::regexp), "expected #{value} to match URI::regexp" }
  end

  def validate_images(data, *required_rels)
    validate_attribute(data, "images", type: Array)
    data["images"].each { |image_data| validate_image(image_data) }

    required_rels.map! { |rel| rel.start_with?("urn:") ? rel : "urn:blinkboxbooks:image:" << rel.tr(" ", "").downcase }
    missing_rels = required_rels - data["images"].map { |image| image["rel"] }
    raise "Required images are missing: #{missing_rels.join(", ")}" unless missing_rels.empty?
  end

  def validate_book(data)
    validate_entity(data, "book")
    validate_attribute(data, "title", type: String) { |value| expect(value).to_not be_empty }
    validate_attribute(data, "publicationDate", type: Date)
    validate_attribute(data, "sampleEligible", type: Boolean)
    validate_images(data, "cover")
    validate_links(data, { rel: "bookpricelist", min: 1, max: 1 },
                         { rel: "contributor", min: 0, max: "∞" },
                         { rel: "publisher", min: 1, max: 1 },
                         { rel: "synopsis", min: 1, max: 1 })
    validate_links(data, rel: "samplemedia", min: 1, max: 1) unless !data["sampleEligible"]
  end

  def validate_book_price(data)
    validate_entity(data, "bookprice")
    validate_attribute(data, "currency", type: String) { |value| expect(value).to match(/[A-Z]{3}/) }
    validate_attribute(data, "price", type: Float) { |value| expect(value).to be >= 0.0 }
    validate_attribute(data, "clubcardPointsAward", type: Integer) { |value| expect(value).to be >= 0 }
    validate_links(data, { rel: "book", min: 1, max: 1 })
  end

  def validate_category(data)
    validate_entity(data, "category")
    validate_attribute(data, "slug", type: String) { |value| expect(value).to_not be_empty }
    validate_attribute(data, "displayName", type: String) { |value| expect(value).to_not be_empty }
    # The ordering sequence number for categories being listed.
    if data["sequence"]
      validate_attribute(data, "sequence", type: Integer) { |value| expect(value).to be >= 0 }
    end
    # The ordering sequence number for categories being listed in a list of recommended categories.
    # This is only returned for results with a filter of recommended
    if data['recommendedSequence']
      validate_attribute(data, "recommendedSequence", type: Integer) { |value| expect(value).to be >= 0 }
    end

    validate_links(data, { rel: "books", min: 1, max: 1 })
  end

  def validate_contributor(data)
    validate_entity(data, "contributor")
    validate_attribute(data, "displayName", type: String) { |value| expect(value).to_not be_empty }
    validate_attribute(data, "sortName", type: String) { |value| expect(value).to_not be_empty }
    validate_attribute(data, "bookCount", type: Integer) { |value| expect(value).to be >= 0 } # TODO: Should this be > 0?
    # TODO: Check for the existence of specific links
    # Cannot use the bellow as the rel for contributor images is
    # urn:blinkboxbooks:image:contributor i.e. no 'schema'
    # validate_links(data)
  end

  def validate_publisher(data)
    validate_entity(data, "publisher")
    validate_attribute(data, "displayName", type: String) { |value| expect(value).to_not be_empty }
    validate_attribute(data, "bookCount", type: Integer) { |value| expect(value).to be >= 0 }
    validate_links(data, { rel: "books", min: 1, max: 1 })
  end

  def validate_synopsis(data)
    validate_entity(data, "synopsis")
    validate_attribute(data, "text", type: String) { |value| expect(value).to_not be_empty }
  end

  def validate_contributor_group(data)
    validate_entity(data, "contributorgroup")
    validate_attribute(data, "nameKey", type: String) { |value| expect(value).to_not be_empty }
    validate_attribute(data, "displayName", type: String) { |value| expect(value).to_not be_empty }
    validate_attribute(data, "startDate", type: Date)
    validate_attribute(data, "endDate", type: Date)
    validate_attribute(data, "enabled", type: String)
    validate_attribute(data, "contributors", type: Array)
  end
end

World(KnowsAboutResponseValidation)