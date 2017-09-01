package indexing

import generated.Blocchiu45impresa
import org.specs2.mutable.Specification
import services.DockerContainerTest

import scala.io.Source
import scala.util.Success

/**
  * Created by fabiana on 28/08/17.
  */
class ElasticClientSpec  extends DockerContainerTest {

  val client = new ElasticClient(port = exposedPort)

  "ElasticClient" should {
    "create correctly an index on ElasticSearch and add a document" in {
      val statusCode = client.putMapping

      statusCode must beEqualTo(200)

      val string = Source.fromResource("pippo_google.xml").getLines.mkString
      val xml = scala.xml.XML.loadString(string)
      val data =  scalaxb.fromXML[Blocchiu45impresa]( xml)

      println(data)

      val statusCode200 = client.addDocument(data, "doc1")
      statusCode200 must beEqualTo(Success(201))


      //client.restClient.performRequest("GET", "", )
    }
  }

}
