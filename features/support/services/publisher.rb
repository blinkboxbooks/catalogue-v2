module KnowsAboutPublishers
  def get_publisher_by_identifier(id)
    http_get :api, "/publishers/#{id}"
  end

  def get_publishers_by_identifiers(ids)
    set_query_param("id", ids)
    http_get :api, "/publishers"
  end

  def get_all_publishers(filters = {})
    filters.each { |key, value| set_query_param(key, value) } unless filters.empty?
    http_get :api, "/publishers"
  end
end

World(KnowsAboutPublishers)