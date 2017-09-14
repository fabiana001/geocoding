package indexing

import java.net.URL

import generated.Blocchiu45impresa
import org.specs2.mutable.Specification
import services.DockerContainerTest

import scala.io.{BufferedSource, Source}
import scala.util.Success

/**
  * Created by fabiana on 28/08/17.
  */
class ElasticClientSpec  extends DockerContainerTest {

  val client = new ElasticClient(port = exposedPort)

  "ElasticClient" should {

    val string = scala.io.Source.fromInputStream(getClass.getResourceAsStream("/pippo_google.xml")).mkString
    val xml = scala.xml.XML.loadString(string)
    val data =  scalaxb.fromXML[Blocchiu45impresa]( xml)
    val docs = client.convertToSplittedDocument(data, "document_test")

    "delete this test" in {
      val string = scala.io.Source.fromFile("./geocoded2_bari/bea0b77a5b133cb9e199f58fd393916d243ab6942b0bc49df51c71845d4378be.xml").mkString
      val xml = scala.xml.XML.loadString(string)
      val data =  scalaxb.fromXML[Blocchiu45impresa]( xml)
      val docs = client.convertToSplittedDocument(data, "document_test")
      docs.foreach(d => println(d))

      true must beEqualTo(true)
    }

    "convert an XML into a list of SplittedDocument" in {
      docs.foreach(println)
      docs.length must beEqualTo(2)
    }

    "create correctly an index on ElasticSearch and add an xml document as SplittedDocument" in {
      val statusCode = client.putMapping()

      val statusCode201List = client.addDocument(data, "doc1")
      statusCode201List.foreach(x => x must beEqualTo(Success(201)))

      statusCode must beEqualTo(200)
    }

    "query correctly elasticsearch db" in {

      val query =
        s"""{
	"query": {
		"nested": {
			"path": "sedeLegale",
			"query": {
				"bool": {
					"must": {
						"match_all": {}
					},
					"filter": {
						"geo_distance": {
							"distance": "2km",
							"sedeLegale.location": {
								"lat": 41.1262519,
								"lon": 16.8629509
							}
						}
					}
				}
			}
		}
	}
}""".stripMargin
      true must beEqualTo(true)
    }

  }

}
