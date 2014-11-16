package SECProject.Quandl

import scalaj.http.{HttpException, HttpOptions, Http}
import akka.actor._

class QuandlActor extends Actor with ActorLogging {
  import QuandlActorProtocol._
  def receive = {
    case _ => log.info("")
  }
}

object QuandlActorProtocol {

}