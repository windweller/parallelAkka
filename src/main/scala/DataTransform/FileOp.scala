package DataTransform

import java.io.{FileReader, BufferedReader}
import FolderReadingNIO.CSVHandler
import com.bizo.mighty.csv.CSVWriter
import scala.collection.mutable.ArrayBuffer
import scala.collection.JavaConversions._

trait FileDB {
  def save(loc: String): Unit

  protected def saveFile(data: ArrayBuffer[Array[String]], outputFile: String): Unit = {
    val output: CSVWriter = CSVWriter(outputFile)
    data.foreach(d => output.write(d))
  }
}

object FileOp {

  abstract class Doc
  case class Text(f: String) extends Doc //"E:\\Allen\\Linguistics\\mTurkNotFutureRawSentence.txt"
  case class TabFile(f: String, data:ArrayBuffer[Array[String]]) extends Doc {

    def this(f: String) = this(f, processTab(f))

    def save() {}
  }

  //by providing default value to data, it's once executed all good pattern
  case class CSV(f: String, header: Boolean, data:ArrayBuffer[Array[String]]) extends Doc {

    def this(f: String, header: Boolean) = this(f, header,processCSV(f, header))

    def save(loc: String): Unit = {
      val output: CSVWriter = CSVWriter(loc)
      data.foreach(r => output.write(r))
    }
  }

  /**
   * Will suffer overhead because we take stuff out and put in Array
   * but probably not a lot (hopefully!)
   * @param loc
   * @param header
   * @return
   */
  def processCSV(loc: String, header: Boolean): ArrayBuffer[Array[String]] = {
    readFile(loc, header, (line) =>  CSVHandler.parseLine(line).toBuffer.toArray)
  }

  def processTab(loc: String): ArrayBuffer[Array[String]] = {
    readFile[Array](loc, header = false, (line) => line.split("\t"))
  }

  def processText(loc: String): Unit = ???

  private def readFile[T[String]](loc: String, header: Boolean, transform: (String) => T[String]): ArrayBuffer[T[String]] = {

    val br = new BufferedReader(new FileReader(loc))
    var line: String = br.readLine
    if (header) line = br.readLine //skip header

    val result = ArrayBuffer[T[String]]()

    while (line != null) {
      result += transform(line)
      line = br.readLine
    }

    result
  }

}
