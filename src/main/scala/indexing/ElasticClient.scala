package indexing

import java.util.Collections
import org.json4s.jackson.Serialization.write
import generated.{Blocchiu45impresa, Datiu45identificativi, Indirizzou45localizzazione, Localizzazione, Localizzazioni}
import indexing.ElasticClient.{AltraSede, Document, SedeLegale}
import org.apache.http.HttpHost
import org.apache.http.entity.ContentType
import org.apache.http.nio.entity.NStringEntity
import org.apache.http.util.EntityUtils
import org.elasticsearch.client.RestClient
import org.json4s.DefaultFormats
import play.libs.Json

import collection.JavaConverters._
import scala.Option
import scala.util.Try

/**
  * Created by fabiana on 24/08/17.
  */
class ElasticClient(host:String = "localhost", port: Int= 9200) {

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
    val pippo = doc.toString


    val response = restClient.performRequest("GET", "/",
      Collections.singletonMap("pretty", "true"))

    System.out.println(EntityUtils.toString(response.getEntity()))

//    //index a document
//    val entity = new NStringEntity(
//      "{\n" +
//        "    \"user\" : \"kimchy\",\n" +
//        "    \"post_date\" : \"2009-11-15T14:12:12\",\n" +
//        "    \"message\" : \"trying out Elasticsearch\"\n" +
//        "}", ContentType.APPLICATION_JSON)

    val e = new NStringEntity( pippo, ContentType.APPLICATION_JSON)
    val documentId = 1
    val indexType = "/twitter/tweet"

    val indexResponse = restClient.performRequest(
      "PUT",
      s"$indexType/$documentId",
      Map.empty[String,String].asJava,
      e)
    indexResponse.getStatusLine.getStatusCode
  }

  private def sendJson() = {

//    val client = HttpClientBuilder.create().build()
//    val request = new HttpPost("http://solrHost.com:8983/solr/update/json")
//    val input = new StringEntity("{\"firstName\":\"Bob\",\"lastName\":\"Williams\"}")
//    input.setContentType("application/json")
//    request.setEntity(input)
//    val response = client.execute(request)
//    val rd = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
//
//    val line = "";
//    while ((line = rd.readLine()) != null) {
//      System.out.println(line);
//    }

  }

  def convertToSolrDocument(blocchiImpresa: Blocchiu45impresa, idDocument: String): Document = {
    val datiIdentificativi: Datiu45identificativi = blocchiImpresa.blocchiu45impresaoption.filter(p => p.key.get == "dati-identificativi").head.as[Datiu45identificativi]
    val localizzazioni = blocchiImpresa.blocchiu45impresaoption.filter(p => p.key.get == "localizzazioni").head.as[Localizzazioni].localizzazione

    val cf = datiIdentificativi.cu45fiscale.getOrElse("ERROR")
    val formaGiuridica = datiIdentificativi.formau45giuridica
    val sede = datiIdentificativi.indirizzou45localizzazione.getOrElse(new Indirizzou45localizzazione)
    val viaSedePrincipale = extractVia(sede)

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
      }  yield (classificazioneAteco.cu45attivita, classificazioneAteco.cu45importanza)

      AltraSede(via, latLng.map(_._1), latLng.map(_._2), attivitaPrimariaEsercitata, attivitaSecondariaEsercitata, classificazioniAteco)
    }

    Document(idDocument, cf, SedeLegale(viaSedePrincipale, sede.lat.getOrElse("-1").toDouble, sede.lng.getOrElse("-1").toDouble), altre_sedi)
  }

  private def extractVia(p: Indirizzou45localizzazione): String = {
      s"${p.toponimo.getOrElse("")} ${p.via.getOrElse("")} ${p.nu45civico.getOrElse("")} ${p.comune.getOrElse("")} ${p.provincia.getOrElse("")} ${p.cap.getOrElse("")} ${p.stato.getOrElse("")}"
  }


}
object ElasticClient {

  case class SedeLegale(via: String, lat: Double, lng: Double)

  case class AltraSede(via: String,
                       lat: Option[Double],
                       lng: Option[Double],
                       attivitaEsercitata: Option[String],
                       attivitaSecondariaEsercitata: Option[String],
                       codiciAteco: Seq[(Option[String], Option[String])])

  case class Document(id: String, cf: String, sedeLegale: SedeLegale, altreSedi: Seq[AltraSede])



}