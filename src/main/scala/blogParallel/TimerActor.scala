package blogParallel

import akka.actor._
import java.io.PrintWriter
import java.text.NumberFormat
import blogParallel.Pattern._

class TimerActor(timerLoc: String) extends Actor with ActorLogging {
  import TimerMsg._

  var totalPCFGTasks = 0
  var totalPatternTasks = 0
  var totalSentenceSplitTasks = 0

  var currentPCFGProgress = 0.0
  var currentPatternProgress = 0.0
  var currentSentenceSplitProgress = 0.0

  val startTime: Long = System.currentTimeMillis()
  var currentTime: Long = 0

  val percentFormat = NumberFormat.getPercentInstance
  percentFormat.setMinimumFractionDigits(2)

  def receive = {
    case TotalTask(num) =>
      totalSentenceSplitTasks = num
      totalPCFGTasks = num

    case UpdatePatternTask(num) =>
      totalPatternTasks += num * (patternFuture.size + patternsPast.size)

    case SentenceSplitAddOne =>
      currentSentenceSplitProgress += 1
      currentTime = System.currentTimeMillis()
      if (currentPCFGProgress % 10 == 0) printToFile()

    case PCFGAddOne =>
      currentPCFGProgress += 1
      currentTime = System.currentTimeMillis()
      if (currentPCFGProgress % 10 == 0) printToFile()

    case PatternAddOne =>
      currentPatternProgress += 1
      currentTime = System.currentTimeMillis()
      if (currentPatternProgress % 10 == 0) printToFile()

    case TestLogging =>
      log.info("test test test!!")

  }


  def printToFile() {


    val writer = new PrintWriter(timerLoc, "UTF-8")
    writer.println("SentenceSplitProgress: "+currentSentenceSplitProgress + " / " + totalSentenceSplitTasks + " => " + percentFormat.format(currentSentenceSplitProgress/totalSentenceSplitTasks))
    writer.println("PCFGProgress: "+currentPCFGProgress + " / " + totalPCFGTasks + " => " + percentFormat.format(currentPCFGProgress/totalPCFGTasks))
    writer.println("PatternProgress: "+currentPatternProgress + " / " + totalPatternTasks + " => " + percentFormat.format(currentPatternProgress/totalPatternTasks))
    val expectedSecs = ((currentTime - startTime) / currentPCFGProgress) * (totalPCFGTasks - currentPCFGProgress)

    writer.println("Spent Time: "+ ((currentTime - startTime)/1000) + "s expected time: " + (expectedSecs/1000) + "s")
    writer.close()
  }

}

object TimerMsg {

  case class TotalTask(numberOfRows: Int)
  case class UpdatePatternTask(newTask: Int)

  case object SentenceSplitAddOne
  case object PCFGAddOne
  case object PatternAddOne
  case object TestLogging
}