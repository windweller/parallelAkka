package dataScienceKaggle

import java.io.BufferedWriter

import akka.actor.{ActorLogging, Actor}


class Printer(writer:BufferedWriter) extends Actor with ActorLogging {
  import PrinterMsg._
  import Common._


  def receive = {
    case Print(vector) =>
      writer.write(vector.mkString(","))
      writer.newLine()
      writer.flush()
  }
}

object PrinterMsg {
  case class Print(result: IndexedSeq[String])
}