package blogParallel

import java.io.File

import com.github.tototoshi.csv.CSVWriter
import Pattern._

object Utility {

  def printCSVHeader(f: File, headers: List[String]) {
    val writer = CSVWriter.open(f, append = false)
    writer.writeRow(headers:::patternFuture:::patternsPast)
    writer.close()
  }

}
