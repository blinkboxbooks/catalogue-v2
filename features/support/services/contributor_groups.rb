module KnowsAboutContributorGroups
  def get_all_contributor_groups(filters = {})
    filters.each { |key, value| set_query_param(key, value) } unless filters.empty?
    http_get :api, "/contributor-groups"
  end

  def get_contributor_group_by_identifier(id)
    http_get :api, "/contributor-groups/#{id}"
  end

end

World(KnowsAboutContributorGroups)