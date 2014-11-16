package SECProject.Database

import akka.actor._

//TODO: Send +1 counter to ProgressCounter
class DatabaseActor extends Actor with ActorLogging {
  import DatabaseActorProtocol._
  import SECProject.SEC.SECDownloadProtocol._

  def receive = {
    case FinalDBSavingRow => log.info("template!")
  }
}

object DatabaseActorProtocol {
  case class FinalDBSavingRow(id: Option[Int], companyName: Option[String], companyAbbr: Option[String], quandlTag: Option[String],
                              companyType: Option[String], CIK: Option[String], formType: Option[String],
                              quarter: Option[String], year: Option[Short], date: Option[String],
                              url: Option[String], fileName: Option[String], docDiskLoc: Option[String],
                              BOWVector: Option[String],ModelVector: Option[String], stockTrend: Option[String])
}