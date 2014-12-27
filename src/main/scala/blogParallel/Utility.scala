package blogParallel

import java.io.File

import com.github.tototoshi.csv.CSVWriter
import Pattern._

/**
 * Created by anie on 12/27/2014.
 */
object Utility {

  def printCSVHeader(f: File, headers: List[String]) {
    val writer = CSVWriter.open(f, append = false)
    writer.writeRow(headers:::patternFuture:::patternsPast)
    writer.close()
  }

}
