package blogParallel

import java.io.File

import akka.actor.{ActorLogging, Actor}
import com.github.tototoshi.csv.CSVWriter


class FilePrintActor(f: File) extends Actor with ActorLogging  {
  import FilePrintMsg._

  def receive = {
    case Print(row, result) =>
      filePrint(row, result, f)
  }

  def filePrint(row: List[String], result: List[Array[Int]], f: File) = {
    println("File writing in progress")
    val writer = CSVWriter.open(f, append = true)

    writer.writeRow(row:::result.flatten.toList)
    writer.close()
  }

}

object FilePrintMsg {
  case class Print(row: List[String], result: List[Array[Int]])
}