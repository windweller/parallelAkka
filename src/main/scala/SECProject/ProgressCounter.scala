package SECProject

import akka.actor._

class ProgressCounter(num: Int) extends Actor with ActorLogging {
  import ProgressCounterProtocol._

  val totalNumSteps = num * 3
  var currentProgress = 0.0
  var currentSEC = 0.0
  var currentQuandl = 0.0
  var currentDatabase = 0.0

  def receive = {
    case CountOneSECDownload =>
      currentProgress += 1; currentSEC +=1
      logPrinting()
    case CountOneQuandl =>
      currentProgress += 1; currentQuandl += 1
      logPrinting()
    case CountOneSavedDatabase =>
      currentProgress += 1; currentDatabase += 1
      logPrinting()
  }

  def logPrinting() {
    log.info("current Total Progress: "+currentProgress/totalNumSteps +
      "| current SEC Download Progress: " + currentSEC/num +
      "| current Quandl Progress: " + currentQuandl/num +
      "| current Database Progress: " + currentDatabase/num)
  }
}

object ProgressCounterProtocol {
  case object CountOneSECDownload
  case object CountOneQuandl
  case object CountOneSavedDatabase
}