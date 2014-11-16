package TwitterFuture

import java.io.PrintWriter
import java.text.NumberFormat

import akka.actor.{ActorLogging, Actor}

class Timer(timerLoc: String) extends Actor with ActorLogging {

  import TimerMsg._

  var totalPCFGTasks = 0
  var currentPCFGProgress = 0.0

  val startTime: Long = System.currentTimeMillis()
  var currentTime: Long = 0

  val percentFormat = NumberFormat.getPercentInstance
  percentFormat.setMinimumFractionDigits(2)

  def receive = {
    case TotalTask(num) =>
      totalPCFGTasks = num
    case PCFGAddOne =>
      currentPCFGProgress += 1
      currentTime = System.currentTimeMillis()
      if (currentPCFGProgress % 10 == 0) printToFile()
  }

  def printToFile() {
    val writer = new PrintWriter(timerLoc, "UTF-8")
    writer.println("PCFGProgress: "+currentPCFGProgress + " => " + percentFormat.format(currentPCFGProgress/totalPCFGTasks))
    val expectedSecs = ((currentTime - startTime) / currentPCFGProgress) * (totalPCFGTasks - currentPCFGProgress)
    writer.println("Spent Time: "+ ((currentTime - startTime)/1000) + "s expected time: " + (expectedSecs/1000) + "s")
    writer.close()
  }

}

object TimerMsg {

  case class TotalTask(numberOfRows: Int)

  case object PCFGAddOne
}