package services

/**
  * Created by fabiana on 28/08/17.
  */
import com.whisk.docker.impl.spotify.DockerKitSpotify
import com.whisk.docker.scalatest.DockerTestKit
import org.scalatest.{FlatSpec, Matchers}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.Matcher
import org.scalatest.time.{Second, Seconds, Span}
import org.specs2._
import org.specs2.specification.core.Env

import scala.concurrent._

class ElasticsearchServiceSpec
  extends FlatSpec
    with Matchers
    with DockerTestKit
    with DockerKitSpotify
    with DockerElasticsearchService {

  implicit val pc = PatienceConfig(Span(20, Seconds), Span(1, Second))

  "elasticserach node" should "be ready with log line checker" in {
    isContainerReady(elasticsearchContainer).futureValue shouldBe true
    //mongodbContainer.getPorts().futureValue.get(27017) should not be empty
    elasticsearchContainer.getIpAddresses().futureValue.foreach(println)
    elasticsearchContainer.getIpAddresses().futureValue should not be Seq.empty
  }
  //def x1 = isContainerReady(elasticsearchContainer) must beTrue.await
}