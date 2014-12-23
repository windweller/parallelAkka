package blogParallel

import akka.actor.{ActorLogging, Actor}
import java.io.{PrintWriter, File}

class ErrorActor(f: String) extends Actor with ActorLogging  {

  import ErrorMsg._

  def receive = {
    case Warning(msg) =>
      val writer = new PrintWriter(f, "UTF-8")
      writer.append(msg + "\r\n")
      writer.close()
  }
}

object ErrorMsg {
  case class Warning(msg: String)
}
