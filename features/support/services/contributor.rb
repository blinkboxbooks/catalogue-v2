module KnowsAboutContributors
  def get_contributor_by_identifier(id)
    http_get :api, "/contributors/#{id}"
  end

  def get_contributors_by_identifiers(ids)
    set_query_param("id", ids)
    http_get :api, "/contributors"
  end

  def get_contributors_by_group_name(name)
    set_query_param("groupname", name)
    http_get :api, "/contributors"
  end
end

World(KnowsAboutContributors)