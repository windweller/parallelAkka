package DataTransform


import FileOp._
import Utils._
import scala.collection.mutable.ArrayBuffer

/**
 * This does not handle repeated columns except commonColumn
 * @param fileA
 * @param fileB
 * @param commonCol if not passed in, we assume it's by the order
 * @tparam T
 * @tparam E
 */
case class JoinFile[T <: Doc, E <: Doc](fileA: T, fileB: E, commonCol: Option[Int]) extends FileDB {

  val dataA = getData(fileA)
  val dataB = getData(fileB)

  //have this because we only want to compute once
  private var combined: Option[ArrayBuffer[Array[String]]] = None

  def getCombined: Option[ArrayBuffer[Array[String]]] = {
    if (combined.isEmpty) computeCombined else combined
  }

  //if files don't have the same size, only matching part will be outputed
  //still, put larger file first, smaller first second
  def computeCombined: Option[ArrayBuffer[Array[String]]] = {

    if (commonCol.nonEmpty) {
      val col = commonCol.get
      val hashedDataA = dataA.view.map{ row =>
        row(col) -> row
      }.toMap //eh this seems slow

      val combinedHashedData = dataB.map { row =>
        hashedDataA(row(col)) ++ dropEle(col, row)
      }
      combined = Some(combinedHashedData)
    }
    else {
      val data = if (dataA.size > dataB.size) {
        dataB.zipWithIndex.map { case (item, index) =>
          dataA(index) ++ item
        }
      }else {
        dataA.zipWithIndex.map { case (item, index) =>
          dataB(index) ++ item
        }
      }
      combined = Some(data)
    }
    combined
  }

  override def save(loc: String): Unit = {
    saveFile(getCombined.get, loc)
    println("File being genereated at " + loc)
  }

  private def getData[D](file:D) = {
    file match {
      case CSV(loc, header, _) => processCSV(loc, header)
      case TabFile(loc, _) => processTab(loc)
    }
  }
}
