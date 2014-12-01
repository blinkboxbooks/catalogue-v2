package com.blinkbox.books.catalogue.common.e2e

import java.nio.file.Files

import com.blinkbox.books.catalogue.common.ElasticsearchConfig
import org.apache.commons.io.FileUtils
import org.elasticsearch.client.Client
import org.elasticsearch.common.settings.ImmutableSettings
import org.elasticsearch.node.NodeBuilder._

class EmbeddedElasticSearch(config: ElasticsearchConfig) {

  private val clusterName = config.clusterName
  private val dataDir = Files.createTempDirectory("elasticsearch_data_").toFile
  private val settings = ImmutableSettings.settingsBuilder
    .put("path.data", dataDir.toString)
    .put("cluster.name", clusterName)
    .build

  private lazy val node = nodeBuilder().local(true).settings(settings).build
  def client: Client = node.client

  def start(): Unit = {
    node.start()
  }

  def stop(): Unit = {
    node.close()

    FileUtils.forceDelete(dataDir)
  }

  def createAndWaitForIndex(index: String): Unit = {
    client.admin.indices.prepareCreate(index).execute.actionGet()
    client.admin.cluster.prepareHealth(index).setWaitForActiveShards(1).execute.actionGet()
  }
}
