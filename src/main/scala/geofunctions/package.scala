/**
  * Created by fabiana on 06/09/17.
  */
package object Geofunctions {

  def entropy(total: Int, categories: Seq[(String, Int)]): Double = {

    val entropy1 = categories.map{x =>
      val v = x._2.toDouble / total
      val logBase2 = java.lang.Math.log(v) / Math.log(2)
      v *  logBase2}

      val entropy = entropy1.sum

    - entropy
  }

  def competitiveness(total: Int, categories: Seq[(String, Int)], atecoCode: String): Double= {
    categories.find(_._1 == atecoCode).map(_._2.toDouble) match {
      case None => -1
      case Some(value)  => - (value / total)
    }
  }


  // def jensenQuality(total: Double, totalTypeA: Double, totalTypeB: Double, )


}
