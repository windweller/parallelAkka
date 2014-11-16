package SECProject

import akka.actor._
import SECProject.Database.DatabaseActorProtocol._
import SECProject.SEC.SECDownloadProtocol._

//You will have two different actor trees (a forest LOL)
//One tree: download FTP
object Main extends App {

    val system = ActorSystem("SECProject")
    val SECDownloader = system.actorOf(Props[JobDelegator])

    val progressCounter = system.actorOf(Props(classOf[ProgressCounter], 100))

    //This method read and compile one line and pass on to process
//    def readAndSaveToDB() {
      //      val projectRoot = "E:/Allen/SECProject/"
      //      val rawSECFolderPrefix = "SEC10KFiles/"
      //      val yahooDoc = projectRoot + "All_USA_Common_Stock.txt"
      //      val quandlDetailInfo = projectRoot + "GOOG_YHOO_North_American_stocks_details.csv"
      //      val quandlWIKI = projectRoot + "WIKI_tickers.csv"
      //      val quandlGOOGYHOO = projectRoot + "GOOG_YHOO_North_American_stocks.csv"
      //
      //    }

      system.shutdown()
  }

  class JobDelegator extends Actor with ActorLogging {

    def SECFileWithoutDownload = ???

    def receive = {
      case FinalDBSavingRow =>
      val databaseActor = context.actorOf(Props[SECProject.Database.DatabaseActor])
      databaseActor ! FinalDBSavingRow

    case SECFileWithDownload =>
      val SECDownloader = context.actorOf(Props[SECProject.SEC.SECDownloader])
      SECDownloader ! SECFileWithDownload

    case SECFileWithoutDownload =>
      val SECDownloader = context.actorOf(Props[SECProject.SEC.SECDownloader])
      SECDownloader ! SECFileWithoutDownload

  }
}

object JobDelegatorProtocol {

}