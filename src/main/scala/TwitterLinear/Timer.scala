package TwitterLinear


import akka.actor._
import java.io.PrintWriter
import java.text.NumberFormat
import TwitterLinear.Pattern._

class Timer(timerLoc: String) extends Actor with ActorLogging {
  import TimerMsg._

  var totalPCFGTasks = 0
  var totalPatternTasks = 0
  var totalSoloTasks = 0

  var currentPCFGProgress = 0.0
  var currentPatternProgress = 0.0
  var currentSoloTasks = 0.0

  val startTime: Long = System.currentTimeMillis()
  var currentTime: Long = 0

  val percentFormat = NumberFormat.getPercentInstance
  percentFormat.setMinimumFractionDigits(2)

  def receive = {
    case TotalTask(num) =>
      totalPCFGTasks = num
      totalPatternTasks = num * (patternFuture.size + patternsPast.size)
    case PCFGAddOne =>
      currentPCFGProgress += 1
      currentTime = System.currentTimeMillis()
      if (currentPCFGProgress % 10 == 0) printToFile()

    case PatternAddOne =>
      currentPatternProgress += 1
      currentTime = System.currentTimeMillis()
      if (currentPatternProgress % 10 == 0) printToFile()

    case SoloAddOne =>
      currentSoloTasks += 1
      currentTime = System.currentTimeMillis()
      if (currentSoloTasks % 10 == 0) printToFileSolo()
  }

  def printToFile() {
    val writer = new PrintWriter(timerLoc, "UTF-8")
    writer.println("PCFGProgress: "+currentPCFGProgress + " => " + percentFormat.format(currentPCFGProgress/totalPCFGTasks))
    writer.println("PatternProgress: "+currentPatternProgress + " => " + percentFormat.format(currentPatternProgress/totalPatternTasks))
    val expectedSecs = ((currentTime - startTime) / (currentPatternProgress + currentPCFGProgress)) * (totalPatternTasks + totalPCFGTasks - currentPatternProgress - currentPCFGProgress)
    writer.println("Spent Time: "+ ((currentTime - startTime)/1000) + "s expected time: " + (expectedSecs/1000) + "s")
    writer.close()
  }

  def printToFileSolo() {
    val writer = new PrintWriter("E:\\Allen\\timer.txt", "UTF-8")
    writer.println("PCFGProgress: "+currentSoloTasks + " => " + percentFormat.format(currentSoloTasks/totalSoloTasks))
    val expectedSecs = ((currentTime - startTime) / currentSoloTasks) * (totalSoloTasks - currentSoloTasks)
    writer.println("Spent Time: "+ ((currentTime - startTime)/1000) + "s expected time: " + (expectedSecs/1000) + "s")
    writer.close()
  }
}

object TimerMsg {

  case class TotalTask(numberOfRows: Int)

  case object PCFGAddOne
  case object PatternAddOne
  case object SoloAddOne
}