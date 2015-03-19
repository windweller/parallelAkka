package NYTAkka

import akka.actor.{ActorRef, ActorPath}

import scala.concurrent.Future
import akka.pattern.pipe

/**
 * Created by anie on 3/13/2015.
 */
class ConcreteWorker(masterLocation: ActorPath) extends Worker(masterLocation) {

  //we'll use current dispatcher for the execution context
  implicit val ec = context.dispatcher

  //Required to be implemented
  override def doWork(workSender: ActorRef, work: Any): Unit = {
    Future {
      workSender ! work
      WorkComplete("done")
    } pipeTo self
  }
}
