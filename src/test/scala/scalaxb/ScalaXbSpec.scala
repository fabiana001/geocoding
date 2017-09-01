package scalaxb

import generated.{Blocchiu45impresa, Datiu45identificativi}
import org.specs2.mutable._

import scala.io.Source

/**
  * Created by fabiana on 23/08/17.
  */
class ScalaXbSpec extends Specification {

  "ScalaXb" should {
    "test serialization and desiarization of XML objects" in {

      val data = scalaxb.fromXML[Datiu45identificativi](
        <dati-identificativi c-fiscale="c8700e741d1bb8b2c74fa8b129a7576670a90dc33a8e31915548fe804214817f" c-fonte="RI" descrizione-tipo-impresa="Societa' di capitale" descrizione-tipo-soggetto="Sede dell'impresa" dt-atto-costituzione="29/10/1999" dt-iscrizione-ri="29/11/1999" fonte="Registro Imprese" tipo-impresa="SC" tipo-soggetto="I">
          <tipi-procedure-concorsuali>
            <tipo-procedura-concorsuale c-tipo="SL">SCIOGLIMENTO E LIQUIDAZIONE</tipo-procedura-concorsuale></tipi-procedure-concorsuali>
          <forma-giuridica c="SR">SOCIETA' A RESPONSABILITA' LIMITATA</forma-giuridica>
          <indirizzo-localizzazione pippo="1" c-comune="006" c-toponimo="VIA" cap="70122" comune="BARI" n-civico="45" provincia="BA" toponimo="VIA" via="S.FRANCESCO D'ASSISI"/>
        </dati-identificativi>
      )

      //println(data)
      val string = scalaxb.toXML[Datiu45identificativi](data, None, Some("dati-identificativi"), generated.defaultScope)
      val cf = (string \ "@c-fiscale").toString

      data.cu45fiscale must beSome(cf)

    }

    "deserialize XML from files" in {

      val string = scala.io.Source.fromInputStream(getClass.getResourceAsStream("example.xml")).mkString
      val xml = scala.xml.XML.loadString(string)
      val data =  scalaxb.fromXML[Blocchiu45impresa]( xml)

      val x = data.blocchiu45impresaoption.filter(p => p.key.get == "dati-identificativi").head.as[Datiu45identificativi]
      val cf = (xml \ "dati-identificativi" \"@c-fiscale").toString
      x.cu45fiscale must beSome(cf)

    }


  }




}
