package blogParallel

import java.io.{FileReader, BufferedReader, File}
import FolderReadingNIO.CSVHandler
import akka.actor.{ActorLogging, Actor, ActorRef}


class TreeReaderActor(timer: ActorRef, tregexActor: ActorRef) extends Actor with ActorLogging {

  import TregexMsg._
  import TimerMsg._
  import TreeReaderMsg._
  import scala.collection.JavaConversions._

  def receive = {
    case CSVFile(loc) =>

      val br = new BufferedReader(new FileReader(loc))
      var line =  br.readLine()
      while (line != null) {
        val lines = CSVHandler.parseLine(line)
        if (lines(0) != "id") {
          tregexActor ! StringMatch(lines.slice(0,7), lines(7))
        }
        line = br.readLine()
      }
  }

}

object TreeReaderMsg {
  case class CSVFile(name:String)
}