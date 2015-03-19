package naccl

import org.scalatest.FlatSpec
import DataTransform.FileOp.CSV
import scala.collection._

/**
 * Created by anie on 3/8/2015.
 */
class TopicCompression extends FlatSpec {

  val fileAddr = "E:\\Allen\\Robert_MW_OldData81\\MTurkAllSentences_FutureTOPICS.csv"

  val file = new CSV(fileAddr, true)

  val map = mutable.Map[String, Array[String]]()


  for (row <- file.data ) {
    if (map.get(row(12)).isEmpty)
      map.put(row(12), row.drop(12))
    else {
      val array = map.get(row(12)).get
      for (c <- 1 to 10) {
        
      }
    }
  }



}
