package geofunctions

import indexing.ElasticClient
import java.lang.Math

import org.json4s._
import org.json4s.jackson.JsonMethods._
import org.apache.http.util.EntityUtils
import org.json4s.JsonAST.JValue

/**
  * Created by fabiana on 05/09/17.
  */
class ElasticGeofunctions(host:String = "localhost", port: Int= 9200, esIndex: String = "test_index", esType: String = "test_type") {
  implicit val formats = DefaultFormats
  val elasticClient = new ElasticClient(host, port, esIndex, esType)



  private def getNeighborsGroupByAteco(lat: Double, lon: Double, distance: Int = 1200): ( Int, Seq[(String, Int)]) = {
    //return the number of place neighbors grouped by Ateco code (we are consigering only the first 2 values, e.g. 56.10)
    val query = s"""{
   "size" : 0,
   "query":{
      "match_all": {

      }
   },
   "aggs":{
      "close_items": {
         "filter": {
             "geo_distance" : {
                    "distance" : "${distance}m",
                    "location" : {
                        "lat" : $lat,
                        "lon" : $lon
                    }
                }
         },
         "aggs": {
            "group_by_codiceAteco":{
               "terms": {
                    "field": "codiceAtecoP.keyword"
                 }
            }
         }
      }
   }
}"""

    val data = elasticClient.query("GET", s"/$esIndex/$esType/_search", query)
    val json = parse(EntityUtils.toString(data.getEntity))

    val total = (json \ "hits" \ "total").extract[Int]

    val buckets = json \ "aggregations" \ "close_items" \ "group_by_codiceAteco" \ "buckets"

    val res = Range(0, total).map{i =>
      val key = (buckets(i) \ "key").extract[String]
      val count = (buckets(i) \ "doc_count").extract[Int]
      (key, count)
    }
    (total, res)
  }

  private def getNeighbors(lat: Double, lon: Double, distance: Int = 1200): Int = {
    val query = s"""{
    "size":  0,
    "query": {
        "bool" : {
            "must" : {
                "match_all" : {}
            },
            "filter" : {
                "geo_distance" : {
                    "distance" : "${distance}m",
                    "location" : {
                        "lat" : $lat,
                        "lon" : $lon
                    }
                }
            }
        }
    }
}"""

    val data = elasticClient.query("GET", s"/$esIndex/$esType/_search", query)


    val json = parse(EntityUtils.toString(data.getEntity))

    val res = (json \ "hits" \ "total").extract[Int]
    res
  }

  private def getNeighborsWithPoints(lat: Double, lon: Double, distance: Int = 1200): (Int, Seq[String]) = {
    val query = s"""{
    "query": {
        "bool" : {
            "must" : {
                "match_all" : {}
            },
            "filter" : {
                "geo_distance" : {
                    "distance" : "${distance}m",
                    "location" : {
                        "lat" : $lat,
                        "lon" : $lon
                    }
                }
            }
        }
    }
}"""
    val data = elasticClient.query("GET", s"/$esIndex/$esType/_search", query)
    val json = parse(EntityUtils.toString(data.getEntity()))

    val total = (json \ "hits" \ "total").extract[Int]
    val hits = json \ "hits" \ "hits"
    val res = Range(0, total).map(i => compact( (hits(i) \ "_source") ))

    (total, res)
  }


  def getDensity(lat: Double, lon: Double, distance: Int = 1200): Int = getNeighbors(lat, lon, distance)

  def getEntropy(lat: Double, lon: Double, distance: Int = 1200): Double = {
    //val density: Int = getDensity(lat,lon, distance)
    val cat: (Int, Seq[(String, Int)]) = getNeighborsGroupByAteco(lat, lon, distance)
    Geofunctions.entropy(cat._1, cat._2)
  }

  def getCompetiveness(lat: Double, lon: Double, distance: Int = 1200, category: String): Double = {
    val query = s"""{
   "size" : 0,
   "query":{
      "match": {
         "codiceAtecoP" : $category
      }
   },
   "aggs":{
      "close_items": {
         "filter": {
             "geo_distance" : {
                    "distance" : "${distance}m",
                    "location" : {
                        "lat" : $lat,
                        "lon" : $lon
                    }
                }
         },
         "aggs": {
            "group_by_codiceAteco":{
               "terms": {
                    "field": "codiceAtecoP.keyword"
                 }
            }
         }
      }
   }
}"""
    val cat =  getNeighborsGroupByAteco(lat, lon, distance)
    Geofunctions.competitiveness(cat._1, cat._2, category)
  }

  def close() = {
    elasticClient.closeClient()
  }

}

object ElasticGeofunctions extends App{
  val es = new ElasticGeofunctions(port = 9900)

  val res1 = es.getNeighbors(41.1262519D, 16.8629509D, 1000)
  val res2 = es.getNeighborsWithPoints(41.1262519D, 16.8629509D, 1000)
  val res3 = es.getNeighborsGroupByAteco(41.1262519D, 16.8629509D, 1000)
  val res4 = es.getEntropy(41.1262519D, 16.8629509D, 1000)
  val res5 = es.getCompetiveness(41.1262519D, 16.8629509D, 1000, "56.10")

  println(
    s"""
       |Num neighbors = $res1
       |Neighbors with point = $res2
       |Neighbors grouped by Ateco codes = $res3
       |Entropy = $res4
       |Competitiveness = $res5
     """.stripMargin)

  es.close()
}
