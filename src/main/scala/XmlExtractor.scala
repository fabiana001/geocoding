import java.io.{File, IOException}
import java.nio.file.Files
import java.util.concurrent.ThreadLocalRandom
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult

import geocoding.GeocodingExceptions.{TooMuchRequestException, WrongCityExeption}
import geocoding.{Geocoding, GisGraphyGeocoding, GoogleGeocoding, MapBoxGeocoding}
import org.slf4j.LoggerFactory
import org.w3c.dom.{Element, Node}
import org.slf4j.Logger

import scala.util.{Failure, Success, Try}
import scala.xml.{Elem, NodeSeq}

/**
  * Created by fabiana on 31/07/17.
  */
object XmlExtractor {

  var lastRequest = System.currentTimeMillis()
  val logger = LoggerFactory.getLogger(XmlExtractor.getClass)

  def delay(millisec: Int) = {
    val diff = System.currentTimeMillis() - lastRequest

    if( diff < millisec ){
      val randomNum = ThreadLocalRandom.current().nextInt(millisec - diff.toInt + 100, millisec + 1000)
      Thread.sleep(randomNum.toLong)
    }
    lastRequest = System.currentTimeMillis()
  }

  def setTag(toponimo: String, via: String, civico:String, cap:String, citta: String, provincia: String, eElement: Element, geocoding: Geocoding): Try[Unit] = {

    val coordinates = geocoding.geocode(toponimo, via, civico, cap, citta, provincia)
    coordinates match {
      case Success((address, (lat, lng))) =>
        eElement.setAttribute("lat", lat.toString)
        eElement.setAttribute("lng", lng.toString)
        eElement.setAttribute("geocodingService", geocoding.getNameService)
        logger.info(s"Successfully extracted address: $address lat-lon: $lat, $lng")
        Success[Unit](())
      case Failure(ex) =>
        val address = s"$toponimo $via $civico, $cap $citta $provincia, italia"
        logger.error(s"Failed to geocode the address ${address} due: ${ex.getMessage}")
        Failure(ex)
    }
  }

  def extractXml(inputFile: String, outputFile: String, geocoding: Geocoding): Unit = {

    val xml: Elem = scala.xml.XML.loadFile(inputFile)
    val pippo: NodeSeq = xml \\ "indirizzo-localizzazione"


    val fXmlFile = new File(inputFile)
    val dbFactory = DocumentBuilderFactory.newInstance()
    val dBuilder = dbFactory.newDocumentBuilder()
    val doc = dBuilder.parse(fXmlFile)

    //optional, but recommended
    //read this - http://stackoverflow.com/questions/13786607/normalization-in-dom-parsing-with-java-how-does-it-work
    doc.getDocumentElement().normalize()

    logger.info("File :" + inputFile)

    val nList = doc.getElementsByTagName("indirizzo-localizzazione")

    Range(0, nList.getLength).foreach{ temp =>

      val nNode = nList.item(temp)

      logger.info(s"\nCurrent Element: < ${nNode.getNodeName} ")

      if (nNode.getNodeType == Node.ELEMENT_NODE) {

        val eElement = nNode.asInstanceOf[Element]

        val string = Range(0, eElement.getAttributes.getLength).map(i => eElement.getAttributes.item(i)).mkString(" ")
        logger.info(s"Current Element: < ${nNode.getNodeName} $string >")

        val toponimo = eElement.getAttribute("toponimo")
        val via = eElement.getAttribute("via")
        val civico = eElement.getAttribute("n-civico")
        val citta = eElement.getAttribute("comune")
        val provincia = eElement.getAttribute("provincia")
        val cap = eElement.getAttribute("cap")

        delay(6500)

        setTag(toponimo, via, civico, cap,citta, provincia, eElement, geocoding) match {
          case Failure(ex) => ex match {

            case _ : TooMuchRequestException =>
              println("ciao")
            case _ : WrongCityExeption =>
              println("ciao")
            case _: java.lang.RuntimeException =>
              println("ciao")

            case _: IOException =>
              println("***************************************ENTRA QUI!!!!***************************************")
              delay(10000)
              setTag(toponimo, via, civico, cap,citta, provincia, eElement, geocoding)

          }
          case Success(_) =>
        }


      }

    }

    val result = new StreamResult(new File(outputFile))
    val transformerFactory = TransformerFactory.newInstance()
    val transformer = transformerFactory.newTransformer()
    val source = new DOMSource(doc)

    transformer.transform(source, result)

    logger.info(s"File saved in $outputFile")

  }

  private def getListOfFiles(d: File): List[File] = {

    if (d.exists && d.isDirectory) {
      d.listFiles.filter(_.isFile).toList
    } else {
      List()
    }
  }

  def main(args: Array[String]): Unit = {
    //extractXml("/Users/fabiana/git/daf/geocoding/src/main/resources/example.xml", "pippo_google.xml", GoogleGeocoding)
    // extractXml("/Users/fabiana/git/daf/geocoding/src/main/resources/example.xml", "pippo_mapbox.xml", MapBoxGeocoding)
    args match {
      case Array(dir) =>
        val d = new File(dir)
        val files = getListOfFiles(d)
        val outDir = new File(s"${d.getParent}/geocoded/")

        if (!outDir.exists()){
          outDir.mkdir()
        } else throw new RuntimeException(s"$outDir yet exists, remove it before to run the program!")


        files.foreach{f =>
          val outFile = s"${outDir.getCanonicalPath}/${f.getName}"
          //Try(extractXml(f.getCanonicalPath, outFile, GisGraphyGeocoding))
         extractXml(f.getCanonicalPath, outFile, GisGraphyGeocoding)
        }//.filter(_.isFailure)
    }

  }

}
