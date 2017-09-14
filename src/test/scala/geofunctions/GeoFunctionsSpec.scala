package geofunctions

import java.math.RoundingMode
import java.text.DecimalFormat

import org.specs2.mutable.Specification

/**
  * Created by fabiana on 08/09/17.
  */
class GeoFunctionsSpec extends Specification{

  private def truncate(value: Double) = Math.floor(value * 100) / 100

  "GeoFunctions" should {
    "elaborate correctly the entropy" in {

      val entropy = Geofunctions.entropy(16, Seq(("a", 9), ("b", 7)))
      truncate(entropy) must beEqualTo(0.98)
    }

    "elaborate correctly the competitiveness" in {
      val competitiveness = Geofunctions.competitiveness(16, Seq(("a", 9), ("b", 7)), "a")
      val competitiveness2 = Geofunctions.competitiveness(16, Seq(("a", 9), ("b", 7)), "c")

      truncate(competitiveness) must beEqualTo(0.56)

      truncate(competitiveness2) must beEqualTo(0)

    }
  }
}
