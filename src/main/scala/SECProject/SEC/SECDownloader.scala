package SECProject.SEC

import akka.actor._
import org.apache.commons.net._
import java.io._

//This class downloads from SEC server
//parallel download
//TODO: Send +1 counter to ProgressCounter
class SECDownloader extends Actor with ActorLogging {
  import SECDownloadProtocol._
  def receive = {
    case SECFileWithDownload => log.info("some stuff")
    case SECFileWithoutDownload => log.info("some stuff")
  }
}

object SECDownloadProtocol {
  case class SECFileWithDownload(companyName: String, fileURL: String, cikNum: Int, formType: String, quarter: String)
  case class SECFileWithoutDownload(companyName: String, fileURL: String, cikNum: Int, formType: String, quarter: String)
}