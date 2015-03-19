package DataTransform

import DataTransform.FileOp.CSV
import DataTransform.Utils._

/**
 * Created by anie on 2/13/2015.
 * right now this is only for Naive Bayes
 */
object VectorConverter {

  object Flag {
    val LAST = 1
  }


  //This always drops 1st, 2nd and last column
  //however, the label will always be preserved, and the label assumed to
  //always be the last column!
  def naiveBayesConvert(csv: CSV, dropColumn: Array[String] => Set[Int]) = {
      csv.data.map(r => r(r.length - 1) -> dropEle[String](dropColumn(r), r))
  }

}
