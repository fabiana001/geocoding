package indexing

import java.util.Collections

import org.json4s.jackson.Serialization.write
import generated.{Blocchiu45impresa, Datiu45identificativi, Indirizzou45localizzazione, Infou45attivita, Localizzazione, Localizzazioni}
import indexing.ElasticClient._
import org.apache.http.HttpHost
import org.apache.http.entity.ContentType
import org.apache.http.nio.entity.NStringEntity
import org.apache.http.util.EntityUtils
import org.elasticsearch.client.{Response, RestClient}
import org.json4s.DefaultFormats
//import play.libs.Json

import collection.JavaConverters._
import scala.Option
import scala.util.Try

/**
  * Created by fabiana on 24/08/17.
  */
class ElasticClient(host:String = "localhost", port: Int= 9200, esIndex: String = "test_index", esType: String = "test_type") {

  val restClient = RestClient.builder(
    new HttpHost(host, port, "http")
   // new HttpHost("localhost", 9201, "http")
  ).build()

  def closeClient() = {
    restClient.close()
  }

  def addDocument(blocchiImpresa: Blocchiu45impresa, idDocument: String): Seq[Try[Int]] = {
    implicit val formats = DefaultFormats

    val docs = convertToSplittedDocument(blocchiImpresa, idDocument)

    docs.map{d =>
      val json = write(d)
      (new NStringEntity( json, ContentType.APPLICATION_JSON),d.id)
    }.map { case(e, id) =>
      Try {
        val indexResponse = restClient.performRequest(
          "PUT",
          s"/$esIndex/$esType/$id",
          Map.empty[String, String].asJava,
          e)
        indexResponse.getStatusLine.getStatusCode
      }
    }

  }

//  def convertToSolrDocument(blocchiImpresa: Blocchiu45impresa, idDocument: String): Document = {
//    val datiIdentificativi: Datiu45identificativi = blocchiImpresa.blocchiu45impresaoption.filter(p => p.key.get == "dati-identificativi").head.as[Datiu45identificativi]
//    val localizzazioni = blocchiImpresa.blocchiu45impresaoption.filter(p => p.key.get == "localizzazioni").head.as[Localizzazioni].localizzazione
//
//    val cf = datiIdentificativi.cu45fiscale.getOrElse("ERROR")
//    val formaGiuridica = datiIdentificativi.formau45giuridica
//    val sede = datiIdentificativi.indirizzou45localizzazione.getOrElse(new Indirizzou45localizzazione)
//    val viaSedePrincipale = extractVia(sede)
//    val latlngSede  = for {
//      l <- sede.lat
//      ln <- sede.lng
//    } yield Location(l.toDouble, ln.toDouble)
//
//    val altre_sedi = localizzazioni.map{ l =>
//      val via = extractVia(l.indirizzou45localizzazione.getOrElse(new Indirizzou45localizzazione))
//      val latLng = for{ indirizzo <- l.indirizzou45localizzazione
//                     lat <- indirizzo.lat
//                     lng <- indirizzo.lng
//      } yield (lat.toDouble, lng.toDouble)
//
//      val attivitaPrimariaEsercitata = l.attivitau45esercitata//.getOrElse("")
//      val attivitaSecondariaEsercitata = l.attivitau45secondariau45esercitata//.getOrElse("")
//
//      val classificazioniAteco = for{
//        classificazioniAteco <- l.classificazioniu45ateco
//        classificazioneAteco <- classificazioniAteco.classificazioneu45ateco
//      }  yield CodiceAteco(classificazioneAteco.cu45attivita, classificazioneAteco.cu45importanza)
//
//      AltraSede(via, latLng.map(l => Location(l._1, l._2)), attivitaPrimariaEsercitata, attivitaSecondariaEsercitata, classificazioniAteco)
//    }
//
//    Document(idDocument, cf, SedeLegale(viaSedePrincipale, latlngSede), altre_sedi)
//  }
  def convertToSplittedDocument(blocchiImpresa: Blocchiu45impresa, idDocument: String): Seq[SplittedDocument] = {
  val datiIdentificativi = blocchiImpresa.blocchiu45impresaoption.filter(p => p.key.get == "dati-identificativi").head.as[Datiu45identificativi]
  val infoAttivita = blocchiImpresa.blocchiu45impresaoption.filter(p => p.key.get == "info-attivita").head.as[Infou45attivita]
  val localizzazioni: Seq[Localizzazione] = blocchiImpresa.blocchiu45impresaoption.filter(p => p.key.get == "localizzazioni") match {
    case Nil => Seq.empty[Localizzazione]
    case head :: _ => head.as[Localizzazioni].localizzazione
  }

  val cf = datiIdentificativi.cu45fiscale.getOrElse("ERROR")
  //val formaGiuridica = datiIdentificativi.formau45giuridica

  val sede = datiIdentificativi.indirizzou45localizzazione.getOrElse(new Indirizzou45localizzazione)
  val viaSedePrincipale = extractVia(sede)
  val latlngSede  = for {
    l <- sede.lat
    ln <- sede.lng
  } yield Location(l.toDouble, ln.toDouble)

  val sedeLegaleAteco: Seq[CodiceAteco] = for{
    classificazioniAteco <- infoAttivita.classificazioniu45ateco
    classificazioneAteco <- classificazioniAteco.classificazioneu45ateco
  }  yield CodiceAteco(classificazioneAteco.cu45attivita, classificazioneAteco.cu45importanza)

  val codiceAtecoP = getCodiceAtecoP(sedeLegaleAteco)

  val sedeLegaleDocument = SplittedDocument(id = idDocument,
    cf = cf,
    isSedeLegale = true,
    via = viaSedePrincipale,
    location = latlngSede,
    attivitaEsercitata = infoAttivita.attivitau45esercitata,
    attivitaSecondariaEsercitata = infoAttivita.attivitau45secondariau45esercitata,
    sedeLegaleAteco,
    codiceAtecoP
  )

  var count = -1

  val altre_sedi = localizzazioni.map{ l =>
    val via = extractVia(l.indirizzou45localizzazione.getOrElse(new Indirizzou45localizzazione))
    val latLng = for{ indirizzo <- l.indirizzou45localizzazione
                      lat <- indirizzo.lat
                      lng <- indirizzo.lng
    } yield (lat.toDouble, lng.toDouble)

    val attivitaPrimariaEsercitata = l.attivitau45esercitata//.getOrElse("")
    val attivitaSecondariaEsercitata = l.attivitau45secondariau45esercitata//.getOrElse("")

    val classificazioniAteco: Seq[CodiceAteco] = for{
      classificazioniAteco <- l.classificazioniu45ateco
      classificazioneAteco <- classificazioniAteco.classificazioneu45ateco
    }  yield CodiceAteco( classificazioneAteco.cu45attivita, classificazioneAteco.cu45importanza)

    //AltraSede(via, latLng.map(l => Location(l._1, l._2)), attivitaPrimariaEsercitata, attivitaSecondariaEsercitata, classificazioniAteco)
    count += 1
    val codiceAtecoP = getCodiceAtecoP(classificazioniAteco)

    SplittedDocument(
      id = s"${idDocument}_$count",
      cf = cf,
      isSedeLegale = false,
      via = via,
      location = latLng.map(l => Location(l._1, l._2)),
      attivitaEsercitata = attivitaPrimariaEsercitata,
      attivitaSecondariaEsercitata = attivitaSecondariaEsercitata,
      classificazioniAteco,
      codiceAtecoP
    )
  }

  altre_sedi :+ sedeLegaleDocument

}

  private def extractVia(p: Indirizzou45localizzazione): String = {
      s"${p.toponimo.getOrElse("")} ${p.via.getOrElse("")} ${p.nu45civico.getOrElse("")} ${p.comune.getOrElse("")} ${p.provincia.getOrElse("")} ${p.cap.getOrElse("")} ${p.stato.getOrElse("")}"
  }

  private def getCodiceAtecoP(list: Seq[CodiceAteco]): Option[String] = {
    list match {
      case Nil => Some("NaN")
      case h :: Nil =>
        h.codice.map(_.split("[.]").take(2).mkString("."))
      case longerList =>
        longerList.find(_.priorita.contains("P")).flatMap(x => x.codice.map(_.split("[.]").take(2).mkString(".")))
    }
  }

  /**
    * Mapping is the process of defining how a document, and the fields it contains, are stored and indexed in *index*
    * @return http request status code
    */
  def putMapping(): Int = {


    val query =
      s"""{
	"settings": {
		"number_of_shards": 1
	},

	"mappings": {
		"$esType": {
			"properties": {
				"id": {
					"type": "text"
				},
				"cf": {
					"type": "text",
					"fields": {
						"keyword": {
							"type": "keyword"
						}
					}
				},
				"isSedeLegale": {
					"type": "boolean"

				},
				"via": {
					"type": "text"
				},
				"location": {
					"type": "geo_point"
				},
				"attivitaEsercitata": {
					"type": "text"
				},
				"attivitaSecondariaEsercitata": {
					"type": "text"
				},
				"codiceAtecoP": {
					"type": "text",
          "fields": {
         	   "keyword": {
         				"type": "keyword"
                }
           }
				},
				"codiciAteco": {
					"type": "nested",
					"properties": {
						"codice": {
							"type": "text",
							"fields": {
								"keyword": {
									"type": "keyword"
								}
							}
						},
						"priorita": {
							"type": "text"
						}
					}
				}

			}
		}
	}
}""".stripMargin

    val entity = new NStringEntity(query, ContentType.APPLICATION_JSON)
    val request = restClient.performRequest("PUT", s"/$esIndex",  Map.empty[String,String].asJava, entity)
    request.getStatusLine.getStatusCode

  }

  def query(method: String = "GET", endpoint: String, query: String = """{"query": {"match_all": {}}}"""): Response = {
    val entity = new NStringEntity(query, ContentType.APPLICATION_JSON)
    val request = restClient.performRequest(method, endpoint,  Map.empty[String,String].asJava, entity)
    request
  }

}

object ElasticClient {

  //case class SedeLegale(via: String, location: Option[Location])

  case class Location(lat: Double, lon:Double)

  case class CodiceAteco( codice: Option[String], priorita: Option[String])

//  case class AltraSede(via: String,
//                       location: Option[Location],
//                       attivitaEsercitata: Option[String],
//                       attivitaSecondariaEsercitata: Option[String],
//                       codiciAteco: Seq[CodiceAteco])

  //case class Document(id: String, cf: String, sedeLegale: SedeLegale, altreSedi: Seq[AltraSede])

  /**
    *
    * @param id
    * @param cf
    * @param isSedeLegale
    * @param via
    * @param location
    * @param attivitaEsercitata
    * @param attivitaSecondariaEsercitata
    * @param codiciAteco
    */
  case class SplittedDocument(id: String,
                              cf: String,
                              isSedeLegale: Boolean,
                              via: String,
                              location: Option[Location],
                              attivitaEsercitata: Option[String],
                              attivitaSecondariaEsercitata: Option[String],
                              codiciAteco: Seq[CodiceAteco],
                              codiceAtecoP: Option[String]
                             )
}