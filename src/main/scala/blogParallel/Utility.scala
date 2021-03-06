package blogParallel

import java.io.{FileReader, BufferedReader, File}
import java.nio.file.{DirectoryStream, Paths, Files, Path}

import com.github.tototoshi.csv.CSVWriter
import Pattern._

import scala.collection.mutable.ListBuffer

object Utility {

  def printCSVHeader(f: File, headers: List[String]) {
    val writer = CSVWriter.open(f, append = false)
    writer.writeRow(headers:::patternFuture:::patternsPast:::patternPresent)
    writer.close()
  }

  /**
   * break docs evenly into multiple folders
   * @param num number of chuncks
   * @param source path for the original dir
   * @param target target path for destination dir
   */
  def breakDocs(num: Int, source: String, target: String): Unit = {
    val fileHandler = FileHandler(source)
    val files = fileHandler.nioTraverseDir[Path]((path) => path)
    val destPath = Paths.get(target)
    (0 to files.length - 1).map { n =>
      val path = destPath.resolve((n % num + 1).toString)
      println("moving: " + files(n).getFileName.toString)
      if (!Files.exists(path)) Files.createDirectory(path)
      Files.copy(files(n), path.resolve(files(n).getFileName))
    }
  }

  def getBreakChunks(source: String): Int = {
    import scala.collection.JavaConversions._
    val directoryStream : DirectoryStream[Path] = Files.newDirectoryStream(Paths.get(source))

    directoryStream.toList.length
  }

  //quickly count lines of a CSV file without truly reading it
  def countCSVLines(loc: String): Int = {
    val reader = new BufferedReader(new FileReader(loc))
    var lines = 0
    while (reader.readLine() != null) lines += 1
    reader.close()
    lines
  }

}
