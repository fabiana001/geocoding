package indexing

import java.io.File

import generated.Blocchiu45impresa
import org.slf4j.LoggerFactory

import scala.util.{Failure, Success}

/**
  * Created by fabiana on 11/09/17.
  */
object IndexingMain extends App{

  val logger = LoggerFactory.getLogger(this.getClass)
  val esClient = new ElasticClient("localhost", 9900, "bari_index", "bari_type")

  esClient.putMapping()

  val dir = new File("/Users/fabiana/git/geocoding/geocoded2_bari")
  val files = getListOfFiles(dir)
  //val outDir = new File(s"${dir.getParent}/geocoded2_bari/")
  files.foreach{f =>
    logger.info(s"Analyzing file ${f.getName}")
    val string = scala.io.Source.fromFile(f.getCanonicalPath).mkString
    val xml = scala.xml.XML.loadString(string)
    val data =  scalaxb.fromXML[Blocchiu45impresa](xml)
    val tryList = esClient.addDocument(data, f.getName)

    val successedDocs = tryList.count(_.isSuccess)
    logger.info(s"File ${f.getName} Inserted $successedDocs / ${tryList.size}")
    tryList.foreach{
      case Failure(ex) => logger.debug(s"File ${f.getName} ${ex.getMessage}")
      case Success(_) =>
    }

  }

  esClient.closeClient()



  private def getListOfFiles(d: File): List[File] = {

    if (d.exists && d.isDirectory) {
      d.listFiles.filter(_.isFile).toList
    } else {
      List()
    }
  }

}
