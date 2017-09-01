package indexing

import java.util.Collections

import org.json4s.jackson.Serialization.write
import generated.{Blocchiu45impresa, Datiu45identificativi, Indirizzou45localizzazione, Localizzazione, Localizzazioni}
import indexing.ElasticClient._
import org.apache.http.HttpHost
import org.apache.http.entity.ContentType
import org.apache.http.nio.entity.NStringEntity
import org.apache.http.util.EntityUtils
import org.elasticsearch.client.RestClient
import org.json4s.DefaultFormats
//import play.libs.Json

import collection.JavaConverters._
import scala.Option
import scala.util.Try

/**
  * Created by fabiana on 24/08/17.
  */
class ElasticClient(host:String = "localhost", port: Int= 9200, indexType: String = "/test_type/test_index") {

  val restClient = RestClient.builder(
    new HttpHost(host, port, "http")
   // new HttpHost("localhost", 9201, "http")
  ).build()

  def closeClient() = {
    restClient.close()
  }

  def addDocument(blocchiImpresa: Blocchiu45impresa, idDocument: String): Try[Int] = Try {
    implicit val formats = DefaultFormats
    val doc = write(convertToSolrDocument(blocchiImpresa, idDocument))

    val e = new NStringEntity( doc, ContentType.APPLICATION_JSON)
    val documentId = 1

    val indexResponse = restClient.performRequest(
      "PUT",
      s"$indexType/$documentId",
      Map.empty[String,String].asJava,
      e)
    indexResponse.getStatusLine.getStatusCode
  }

  def convertToSolrDocument(blocchiImpresa: Blocchiu45impresa, idDocument: String): Document = {
    val datiIdentificativi: Datiu45identificativi = blocchiImpresa.blocchiu45impresaoption.filter(p => p.key.get == "dati-identificativi").head.as[Datiu45identificativi]
    val localizzazioni = blocchiImpresa.blocchiu45impresaoption.filter(p => p.key.get == "localizzazioni").head.as[Localizzazioni].localizzazione

    val cf = datiIdentificativi.cu45fiscale.getOrElse("ERROR")
    val formaGiuridica = datiIdentificativi.formau45giuridica
    val sede = datiIdentificativi.indirizzou45localizzazione.getOrElse(new Indirizzou45localizzazione)
    val viaSedePrincipale = extractVia(sede)
    val latlngSede  = for {
      l <- sede.lat
      ln <- sede.lng
    } yield Location(l.toDouble, ln.toDouble)

    val altre_sedi = localizzazioni.map{ l =>
      val via = extractVia(l.indirizzou45localizzazione.getOrElse(new Indirizzou45localizzazione))
      val latLng = for{ indirizzo <- l.indirizzou45localizzazione
                     lat <- indirizzo.lat
                     lng <- indirizzo.lng
      } yield (lat.toDouble, lng.toDouble)

      val attivitaPrimariaEsercitata = l.attivitau45esercitata//.getOrElse("")
      val attivitaSecondariaEsercitata = l.attivitau45secondariau45esercitata//.getOrElse("")

      val classificazioniAteco = for{
        classificazioniAteco <- l.classificazioniu45ateco
        classificazioneAteco <- classificazioniAteco.classificazioneu45ateco
      }  yield CodiceAteco(classificazioneAteco.cu45attivita, classificazioneAteco.cu45importanza)

      AltraSede(via, latLng.map(l => Location(l._1, l._2)), attivitaPrimariaEsercitata, attivitaSecondariaEsercitata, classificazioniAteco)
    }

    Document(idDocument, cf, SedeLegale(viaSedePrincipale, latlngSede), altre_sedi)
  }

  private def extractVia(p: Indirizzou45localizzazione): String = {
      s"${p.toponimo.getOrElse("")} ${p.via.getOrElse("")} ${p.nu45civico.getOrElse("")} ${p.comune.getOrElse("")} ${p.provincia.getOrElse("")} ${p.cap.getOrElse("")} ${p.stato.getOrElse("")}"
  }

  def putMapping: Int = {

    val query = """{
   "settings" : {
      "number_of_shards" : 1 },

   "mappings":{
      "test_type":{
         "properties":{
            "id":{
               "type":"text"
            },
            "cf":{
               "type":"text"
            },
            "sedeLegale":{
               "type":"nested",
               "properties":{
                  "via":{
                     "type":"text"
                  },
                  "location":{
                     "type":"geo_point"
                  }
               }
            },
            "altreSedi":{
               "properties":{
                  "via":{
                     "type":"text"
                  },
                  "location":{
                     "type":"geo_point"
                  },
                  "attivitaEsercitata":{
                     "type":"text"
                  },
                  "attivitaSecondariaEsercitata":{
                     "type":"text"
                  },
                  "codiciAteco":{
                     "type":"text"
                  }
               }
            }
         }
      }
   }
}"""

    val entity = new NStringEntity(query, ContentType.APPLICATION_JSON)
    val request = restClient.performRequest("PUT", "test_index",  Map.empty[String,String].asJava, entity)
    request.getStatusLine.getStatusCode

  }

}

object ElasticClient {

  case class SedeLegale(via: String, location: Option[Location])

  case class Location(lat: Double, lon:Double)

  case class CodiceAteco( codice: Option[String], priorita: Option[String])

  case class AltraSede(via: String,
                       location: Option[Location],
                       attivitaEsercitata: Option[String],
                       attivitaSecondariaEsercitata: Option[String],
                       codiciAteco: Seq[CodiceAteco])

  case class Document(id: String, cf: String, sedeLegale: SedeLegale, altreSedi: Seq[AltraSede])
}