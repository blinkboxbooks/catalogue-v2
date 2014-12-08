module KnowsAboutCategories
  def get_category_by_identifier(id)
    http_get :api, "/categories/#{id}"
  end

  def get_category_by_slug(slug)
    set_query_param("slug", slug)
    http_get :api, "/categories"
  end

  def get_all_categories(filters = {})
    filters.each { |key, value| set_query_param(key, value) } unless filters.empty?
    http_get :api, "/categories"
  end

  def get_category_by_location(location)
    set_query_param("location", location)
    http_get :api, "/categories"
  end

  def get_categories_by_kind(kind)
    set_query_param("kind", kind)
    http_get :api, "/categories"
  end

  def get_recommended_categories()
    set_query_param("recommended", true)
    http_get :api, "/categories"
  end

  def get_non_recommended_categories()
    set_query_param("recommended", false)
    http_get :api, "/categories"
  end
end

World(KnowsAboutCategories)