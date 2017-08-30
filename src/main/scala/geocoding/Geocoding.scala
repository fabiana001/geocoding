package geocoding

import java.math.BigInteger
import java.net.{HttpURLConnection, URL}

import com.sun.corba.se.spi.transport.ReadTimeouts
import geocoding.GeocodingExceptions.TooMuchRequestException

import scala.util.{Failure, Success, Try}

/**
  * Created by fabiana on 01/08/17.
  */
trait Geocoding {

  def getNameService: String

  def geocode(toponimo: String,
              via: String,
              numero: String,
              cap: String,
              citta: String,
              provincia: String,
              stato: String = "Italia",
              connectTimeout: Int = 30000,
              readTimeout: Int = 30000,
              requestMethod: String = "GET"): Try[( String, (Double, Double))]

  def connect(url: String, connectTimeout: Int, readTimeout: Int, requestMethod: String = "GET"): Try[String] = {
    val connection = new URL(url).openConnection.asInstanceOf[HttpURLConnection]
    connection.setConnectTimeout(connectTimeout)
    connection.setReadTimeout(readTimeout)
    connection.setRequestMethod(requestMethod)

    connection.getResponseCode match {
      case 200 =>
        val inputStream = connection.getInputStream
        val content = scala.io.Source.fromInputStream(inputStream).mkString
        if (inputStream != null) inputStream.close()
          Success(content)
      case 503 => Failure(TooMuchRequestException())
      case state => Failure(new RuntimeException(s"Http status code: $state"))
    }


  }

  def convertAddress(address:String): String = {
    val hex = String.format("%02x", new BigInteger(1, address.getBytes))
    val slidedHex = hex.toList.sliding(2,2).map(_.mkString).mkString("%")
    s"%$slidedHex"
  }
}
