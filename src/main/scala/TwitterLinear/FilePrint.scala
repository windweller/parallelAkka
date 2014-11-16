package TwitterLinear

import akka.actor._
import com.github.tototoshi.csv._
import java.io.File
import Entry._
import edu.stanford.nlp.trees.Tree

class FilePrint extends Actor with ActorLogging {
  import FilePrintMsg._

  def receive = {
    case Print(row, result, f) =>
      filePrint(row, result, f)
    case PrintAll(result, f) =>
      filePrintAll(result, f)
  }
}

object FilePrintMsg {
  case class Print(row: List[String], result: List[Array[Int]], f: File)
  case class PrintAll(result: List[(List[String], Array[Int])], f: File)
}