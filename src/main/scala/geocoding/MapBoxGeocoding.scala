package geocoding

import java.math.BigInteger
import java.net.{HttpURLConnection, URL}
import org.json4s._
import org.json4s.jackson.JsonMethods._
import com.typesafe.config.ConfigFactory

import scala.util.{Failure, Success, Try}

/**
  * Created by fabiana on 31/07/17.
  */
object MapBoxGeocoding extends Geocoding{

  val delay = 6500

  val mapboxAccessToken = ConfigFactory.load().getString("geocoding.mapboxAccessToken")

  override def geocode(toponimo: String,
                       via: String,
                       numero: String,
                       cap: String,
                       citta: String,
                       provincia: String,
                       stato: String = "Italy",
                       connectTimeout: Int = 5000,
                       readTimeout: Int = 5000,
                       requestMethod: String = "GET"): Try[( String, (Double, Double))] = {


    val address = s"$toponimo $via $numero, $cap $citta $provincia, $stato"

    val query = convertAddress(address)
    val url = s"https://api.tiles.mapbox.com/geocoding/v5/mapbox.places/$query.json?language=it&limit=2&access_token=$mapboxAccessToken"

    println(url)
    val connection = new URL(url).openConnection.asInstanceOf[HttpURLConnection]
    connection.setConnectTimeout(connectTimeout)
    connection.setReadTimeout(readTimeout)
    connection.setRequestMethod(requestMethod)

    connection.getResponseCode match {
      case 200 =>
        val inputStream = connection.getInputStream
        val content = scala.io.Source.fromInputStream(inputStream).mkString
        if (inputStream != null) inputStream.close()
        extractCoordinates(content, cap.toInt).map(x => (address, (x._1, x._2)))
      case state => Failure(new RuntimeException(s"Http status code: $state"))
    }

  }

  private def extractCoordinates(response: String, inputCap: Int): Try[(Double, Double)] = {
    implicit val formats = DefaultFormats
    val json = parse(response)
    val res = (json \ "features") (0) \"geometry" \ "coordinates"
    val relevance = ((json \ "features") (0) \ "relevance").toOption.getOrElse(JDouble(-1)).extract[Double]
    val extractedCap = (json \ "context" \ "text").toOption.getOrElse(JDouble(-1)).extract[Double]

    if(relevance > 0.60) {
      val lat = res (1).extract[Double]
      val lon = res (0).extract[Double]
      Success((lat, lon))
    }
    else Failure( new RuntimeException("Address not found"))
  }

  def main(args: Array[String]): Unit = {
    println(MapBoxGeocoding.geocode("via", "S.Francesco D'Assisi","45", "70122","Bari","BA"))
    //println(MapBoxGeocoding.geocode("via", "Gian Lorenzo Bernini","7", "70121","Barletta","Bt"))

  }

  override def getNameService: String = "mapboxapis"
}
