package geocoding

import java.math.BigInteger
import java.net.{HttpURLConnection, URL}
import com.typesafe.config.ConfigFactory
import play.api.libs.json.Json

import scala.util.{Failure, Try}

/**
  * Created by fabiana on 31/07/17.
  */
object GoogleGeocoding extends Geocoding{

  val googleAccessToken = ConfigFactory.load().getString("geocoding.googleAccessToken")


  override def geocode(toponimo: String,
              via: String,
              numero: String,
              cap: String,
              citta: String,
              provincia: String,
              stato: String = "Italia",
              connectTimeout: Int = 5000,
              readTimeout: Int = 5000,
              requestMethod: String = "GET"): Try[(String, (Double, Double))] = {


    val address = s"$toponimo $via $numero, $cap, $citta $provincia, $stato"

    val query = convertAddress(address)
    val url = s"https://maps.googleapis.com/maps/api/geocode/json?address=$query&key=$googleAccessToken"

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
        extractCoordinates(content).map(x => (address, (x._1, x._2)))
      case state => Failure(new RuntimeException(s"Http status code: $state"))
    }

  }

  private def extractCoordinates(response: String) = {
    val json = Json.parse(response)
    val status = (json \ "status").get.as[String]
    if (status == "OK") Try {
      val res = json \ "results" \ 0 \ "geometry" \ "location"
      val lat = (res \ "lat").get.as[Double]
      val lon = (res \ "lng").get.as[Double]
      (lat, lon)
    }
    else Failure( new RuntimeException(s"Error request $status"))

  }

  def main(args: Array[String]): Unit = {
    println(GoogleGeocoding.geocode("via", "S.Francesco D'Assisi","45", "70122","Bari","BA"))
  }

  override def getNameService: String = "googleapis"
}
