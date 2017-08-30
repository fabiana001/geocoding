package services

import com.spotify.docker.client.{DefaultDockerClient, DockerClient}
import com.whisk.docker.impl.spotify.SpotifyDockerFactory
import com.whisk.docker.{DockerContainer, DockerFactory, DockerKit, DockerReadyChecker}
import org.scalatest.{BeforeAndAfterAll, Suite}

/**
  * Created by fabiana on 28/08/17.
  */
import scala.concurrent.duration._

trait DockerElasticsearchService extends DockerKit {

  val DefaultElasticsearchHttpPort = 9200
  val DefaultElasticsearchClientPort = 9300

  val elasticsearchContainer = DockerContainer("elasticsearch:1.7.1")
    .withPorts(DefaultElasticsearchHttpPort -> Some(DefaultElasticsearchHttpPort),
      DefaultElasticsearchClientPort -> Some(DefaultElasticsearchClientPort))
    .withReadyChecker(
      DockerReadyChecker
        .HttpResponseCode(DefaultElasticsearchHttpPort, "/")
        .within(100.millis)
        .looped(20, 1250.millis)
    )
//  DockerReadyChecker.LogLineContains("The server is now ready to accept connections"))

  abstract override def dockerContainers: List[DockerContainer] =
    elasticsearchContainer :: super.dockerContainers
}