package com.blinkbox.books.catalogue.common.e2e

import java.nio.file.Files

import com.blinkbox.books.catalogue.common.ElasticsearchConfig
import org.apache.commons.io.FileUtils
import org.elasticsearch.client.Client
import org.elasticsearch.client.Requests
import org.elasticsearch.common.Priority
import org.elasticsearch.common.settings.ImmutableSettings
import org.elasticsearch.common.unit.TimeValue
import org.elasticsearch.node.NodeBuilder._

class EmbeddedElasticSearch(config: ElasticsearchConfig) {

  private val clusterName = config.clusterName
  private val dataDir = Files.createTempDirectory("elasticsearch_data_").toFile
  private val settings = ImmutableSettings.settingsBuilder
    .put("path.data", dataDir.toString)
    .put("cluster.name", clusterName)
    .put("index.store.type", "memory")
    .put("index.number_of_shards", 1)
    .put("index.number_of_replicas", 0)
    .put("discovery.zen.ping.multicast.enabled", false)
    .build

  private lazy val node = nodeBuilder().local(true).settings(settings).build
  def client: Client = node.client

  def waitGreen(): Unit = {
    val actionGet = client.admin.cluster.health(
      Requests
        .clusterHealthRequest("_all")
        .timeout(TimeValue.timeValueSeconds(300))
        .waitForGreenStatus()
        .waitForEvents(Priority.LANGUID)
        .waitForRelocatingShards(0)).actionGet

    if (actionGet.isTimedOut) sys.error("The ES cluster didn't go green within the extablished timeout")
  }

  def start(): Unit = {
    node.start()
    waitGreen()
  }

  def stop(): Unit = {
    node.close()
    FileUtils.forceDelete(dataDir)
  }
}
