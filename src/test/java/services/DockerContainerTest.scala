package services

import java.util.{Collections, UUID}

import org.apache.http.HttpHost
import org.apache.http.util.EntityUtils
import org.elasticsearch.client.{Response, RestClient}
import org.specs2.mutable._
import org.specs2.specification.BeforeAfterAll

/**
  * Created by fabiana on 30/08/17.
  */
class DockerContainerTest extends Specification with BeforeAfterAll {

  var containerId: String = _
  val exposedPort = 9900

  "DockerContainerTest" should {
    "execute correctly elasticsearch" in {

      println(containerId)

      //wait elasticsearch service is up
      println(s"Before ${System.currentTimeMillis()}")
      Thread.sleep(40000L)

      println(s"After ${System.currentTimeMillis()}")
      val restClient = RestClient.builder(
        new HttpHost("localhost", exposedPort, "http")
      ).build()

      val response: Response = restClient.performRequest("GET", "/",
        Collections.singletonMap("pretty", "true"))

      val string = EntityUtils.toString(response.getEntity())

      println(s"$string")

      containerId must not be null
      response.getStatusLine.getStatusCode must beEqualTo(200)
    }
  }


  override def beforeAll(): Unit = {
    containerId = DockerContainer.builder()
      //.withImage("docker.elastic.co/elasticsearch/elasticsearch:5.4.1")
      .withImage("elasticsearch:latest")
      //for test use unconvetional ports
      .withPort(s"$exposedPort/tcp", "9200/tcp")
      //.withEnv("http.host", "0.0.0.0")
      .withEnv("network.host", "0.0.0.0")
      .withEnv("transport.host", "127.0.0.1")
      .withName("TEST-" + UUID.randomUUID())
      .run()
  }

  override def afterAll(): Unit = {
    DockerContainer.builder().withId(containerId).clean()
  }
}
