package blogParallel

import akka.actor._
import java.io.PrintWriter
import java.text.NumberFormat
import blogParallel.Pattern._

class TimerActor(timerLoc: String) extends Actor with ActorLogging {
  import TimerMsg._
  import context._

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
      totalPatternTasks += num * (patternFuture.size + patternsPast.size + patternPresent.size)

    case SentenceSplitAddOne =>
      currentSentenceSplitProgress += 1
      currentTime = System.currentTimeMillis()
      if (currentPCFGProgress % 5000 == 0) printToFile()

    case PCFGAddOne =>
      currentPCFGProgress += 1
      currentTime = System.currentTimeMillis()
      if (currentPCFGProgress % 5000 == 0) printToFile()

    case PatternAddOne =>
      currentPatternProgress += 1
      currentTime = System.currentTimeMillis()
      if (currentPatternProgress % 5000 == 0) printToFile()

    case QMode =>
      currentPCFGProgress = totalPCFGTasks
      currentSentenceSplitProgress = totalSentenceSplitTasks
      become(qmode)

    case TestLogging =>
      log.info("test test test!!")

  }

  def qmode: Receive = {

    case TotalTask(num) =>
      totalSentenceSplitTasks = num
      totalPCFGTasks = num
      totalPatternTasks = num * (patternFuture.size + patternsPast.size + patternPresent.size)

    case PatternAddOne =>
      currentPatternProgress += 1
      currentTime = System.currentTimeMillis()
      if (currentPatternProgress % 5000 == 0) printToFileByPattern()

    case UpdatePatternTask(num) =>
      totalPatternTasks += num * (patternFuture.size + patternsPast.size + patternPresent.size)

    case NMode =>
      unbecome()
  }

  def printToFileByPattern(): Unit = {
    val writer = new PrintWriter(timerLoc, "UTF-8")
    writer.println("PatternProgress: "+currentPatternProgress + " / " + totalPatternTasks + " => " + percentFormat.format(currentPatternProgress/totalPatternTasks))
    val expectedSecs = ((currentTime - startTime) / currentPatternProgress) * (totalPatternTasks - currentPatternProgress)

    writer.println("Spent Time: "+ ((currentTime - startTime)/1000) + "s expected time: " + (expectedSecs/1000) + "s")
    writer.close()
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

  case object NMode
  case object QMode
  case object SentenceSplitAddOne
  case object PCFGAddOne
  case object PatternAddOne
  case object TestLogging
}