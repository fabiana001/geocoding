package geocoding

/**
  * Created by fabiana on 03/08/17.
  */
object GeocodingExceptions {

  final case class TooMuchRequestException() extends RuntimeException("Request returns with status code: 503")

  final case class WrongCityExeption(msg: String) extends RuntimeException(msg)

}
