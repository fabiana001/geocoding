package geocoding
import geocoding.GeocodingExceptions.{TooMuchRequestException, WrongCityExeption}
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.json4s._
import org.json4s.jackson.JsonMethods._
import scala.util.{Failure, Success, Try}

/**
  * Created by fabiana on 02/08/17.
  */
object GisGraphyGeocoding extends Geocoding {

  val delay = 6500

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
      case Success(data) =>
        extractCoordinates(data, citta).map( x => (address, (x._1, x._2)))
      case Failure(ex) => Failure(ex)
    }

  }

  private def extractCoordinates(response: String, city: String) = {
    implicit val formats = DefaultFormats

    val json: JValue = parse(response)
    val res = (json \ "result")(0)
    val extractedCity = (res \ "state").extractOpt[String].getOrElse("ERROR COUNTRY")
    val lat = (res \ "lat").toOption.getOrElse(JDouble(-1)).extract[Double]
    val lng = (res \ "lng").toOption.getOrElse(JDouble(-1)).extract[Double]

    logger.info(response)
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
