package geocoding
import geocoding.GeocodingExceptions.{TooMuchRequestException, WrongCityExeption}
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import play.api.libs.json.{JsNumber, JsString, Json}

import scala.util.{Failure, Success, Try}

/**
  * Created by fabiana on 02/08/17.
  */
object GisGraphyGeocoding extends Geocoding {

  val logger = LoggerFactory.getLogger(GisGraphyGeocoding.getClass)
  override def getNameService: String = "gisgraphapis"

  private def checkCity(extractedCity: String, requestedString: String) = {
    extractedCity.toLowerCase == requestedString.toLowerCase
  }

  override def geocode(toponimo: String, via: String, numero: String, cap: String, citta: String, provincia: String, stato: String, connectTimeout: Int, readTimeout: Int, requestMethod: String): Try[(String, (Double, Double))] = {

    val address = s"$toponimo $via $numero $cap $citta $provincia IT"
    val hexAddress = convertAddress(address)
    val url = s"http://services.gisgraphy.com/geocoding/geocode?address=$hexAddress&format=json"
    logger.info(url)

    connect(url, connectTimeout,readTimeout) match {
      case Success(data) => extractCoordinates(data, citta).map( x => (address, (x._1, x._2)))
      case Failure(ex) => Failure(ex)
    }

  }

  private def extractCoordinates(response: String, city: String) = {
    val json = Json.parse(response)
    val res = json \ "result" \ 0
    val extractedCity = (res \ "state").getOrElse(JsString(" ")).as[String]
    val lat = (res \ "lat").getOrElse(JsNumber(-1)).as[Double]
    val lng = (res \ "lng").getOrElse(JsNumber(-1)).as[Double]

    logger.info(res.get.toString())
    if(lat != -1 && lng != -1) {
      if (checkCity(extractedCity, city))
        Success((lat, lng))
      else Failure(WrongCityExeption(s"Extracted city $extractedCity is not equal to the input city $city"))
    }
    else
      Failure( new RuntimeException(s"Error during coordinates extraction process"))

  }

  def main(args: Array[String]): Unit = {
    println(GisGraphyGeocoding.geocode("via", "S.Francesco D'Assisi","45", "70122","Bari","BA"))
  }
}
