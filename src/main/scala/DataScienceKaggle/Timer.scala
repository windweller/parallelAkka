package dataScienceKaggle

import java.io.PrintWriter
import java.text.NumberFormat

import akka.actor.{ActorRef, ActorLogging, Actor}

class Timer(timerLoc: String, printer:ActorRef) extends Actor with ActorLogging {
  import TimerMsg._
  import PrinterMsg._

  var currentRows = 0.0
  var totalRows = 300000 //two processes

  val startTime: Long = System.currentTimeMillis()
  var currentTime: Long = 0

  val percentFormat = NumberFormat.getPercentInstance
  percentFormat.setMinimumFractionDigits(5)

  def receive = {
    case TotalTask(num) =>
      totalRows = num

    case OneDone =>
      currentRows += 1
      currentTime = System.currentTimeMillis()
      if (currentRows % 1000 == 0 || currentRows == totalRows) printToFile()
      //sepcial task
//      if (currentRows == totalRows) printDic()
  }

  def printToFile(): Unit = {
    val writer = new PrintWriter(timerLoc, "UTF-8")

    writer.println("PatternProgress: "+currentRows + " / " + totalRows + " => " + percentFormat.format(currentRows/totalRows))
    val expectedSecs = ((currentTime - startTime) / currentRows) * (totalRows - currentRows)

    writer.println("Spent Time: "+ ((currentTime - startTime)/1000) + "s expected time: " + (expectedSecs/1000) + "s")
    writer.close()
  }

  def printDic(): Unit = {
    import Common._

    log.info("length of dictionary: " + dictionaries.length)

    val writer = new PrintWriter("E:\\Allen\\DataScience\\vectorizedTrain.csv", "UTF-8")
    dictionaries.map { d =>
     writer.println(d._1 + "," + d._2.toList.mkString(","))
    }

    writer.close()
  }

}

object TimerMsg {
  case class TotalTask(num: Int)
  case object OneDone
}