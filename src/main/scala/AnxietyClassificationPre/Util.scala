package AnxietyClassificationPre

/**
 * Created by anie on 7/10/2014.
 */
object Util {
  implicit def string2Int(s: String): Int = augmentString(s).toInt
}
