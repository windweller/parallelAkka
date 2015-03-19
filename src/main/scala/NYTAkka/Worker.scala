package NYTAkka

import akka.actor.{ActorRef, Actor, ActorLogging, ActorPath}

/**
 * Created by anie on 3/13/2015.
 */
abstract class Worker(masterLocation: ActorPath)
                                  extends Actor with ActorLogging {
  import MasterWorkerProtocol._

  //We need to know where the master is
  val master = context.actorSelection(masterLocation)

  //This is how our derivations will interact with us. It
  //allows derivations to complete work asynchronously
  case class WorkComplete(result: Any)

  //Required to be implemented
  def doWork(workSender: ActorRef, work: Any): Unit

  //Notify the Master that we're alive!
  override def preStart() = master ! WorkerCreated(self)

  //This is the state we're in when we're working on something
  //In this state we deal with messages in a much more
  //reasonable manner

  def working(work: Any): Receive = {
    //Pass... we're already working
    case WorkIsReady =>
    //Pass... we're already working
    case WorkToBeDone =>
    //Pass... we shouldn't even get this
    case WorkToBeDone(_) =>
      log.error("Yikes. Master told me to do work, while I'm working")
    //Our derivation has completed its task
    case WorkComplete(result) =>
      log.info("Work is complete. Result {}.", result)
      master ! WorkIsDone(self)
      master ! WorkerRequestWork(self)
      //We're idle now
      context.become(idle)
  }

  //When idle, only two messages will be applicable
  def idle: Receive = {
    //Master says there's work to be done, let's ask for it
    case WorkIsReady =>
      log.info("Requesting work")
      master ! WorkerRequestWork(self)
    //Send the work off to the implementation
    case WorkToBeDone(work) =>
      doWork(sender(), work)
      context.become(working(work))
    //We asked for it
    case NoWorkToBeDone =>
  }

  def receive = idle

}
