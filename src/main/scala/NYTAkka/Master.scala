package NYTAkka

import akka.actor.Actor.Receive
import akka.actor.{Terminated, ActorLogging, Actor, ActorRef}

/**
 * Created by anie on 3/13/2015.
 */
class Master extends Actor with ActorLogging{
  import MasterWorkerProtocol._


  import scala.collection.mutable.{Map, Queue}

  //Holds known workers and what they may be working on
  val workers = Map.empty[ActorRef, Option[(ActorRef, Any)]]

  //Holds the incoming list of work to be done as well
  //as the memory of who asked for it

  val workQ = Queue.empty[(ActorRef, Any)]

  //Notifies workers there's work available
  def notifyWorkers(): Unit = {
    if (workQ.nonEmpty) {
      workers.foreach {
        case (worker, m) if m.isEmpty => worker ! WorkIsReady
        case _ =>
      }
    }
  }

  override def receive: Receive = {
    //Worker is alive. Add to list. Watch for death, let him know if there's work to be done
    case WorkerRequestWork(worker) =>
      log.info("Worker requests work: {}", worker)
      if (workers.contains(worker)) {
        if (workQ.isEmpty) worker ! NoWorkToBeDone
        else if (workers(worker) == None) {
          val (workSender, work) = workQ.dequeue()
          workers += (worker -> Some(workSender, work))
          //use the special form of 'tell' that lets us supply the sender
          worker.tell(WorkToBeDone(work), workSender)
        }
      }

    //Worker has completed its work and we can clear it out
    case WorkIsDone(worker) =>
      if (!workers.contains(worker)) log.error("Blurgh! {} said it's done but we didn't know about him", worker)
      else workers += (worker -> None)

    //A worker died. If he was doing anything then we need to give it to
    //someone else so we just add it back to the master
    //and let things progress as usual
    case Terminated(worker) =>
      if (workers.contains(worker) && workers(worker) != None) {
        log.error("Blurgh! {} died while processing {}", worker, workers(worker))
        val (workSender, work) = workers(worker).get
        self.tell(work, workSender)
      }
      workers -= worker

    //covers anything other than our protocol (work to be done)
    case work =>
      log.info("Queueing {}", work)
      workQ.enqueue(sender -> work)
      notifyWorkers()
  }

}

object MasterWorkerProtocol {
  //Messages from Workers
  case class WorkerCreated(worker: ActorRef)
  case class WorkerRequestWork(worker: ActorRef)
  case class WorkIsDone(worker: ActorRef)

  //Messages to Workers
  case class WorkToBeDone(work: Any)
  case object WorkIsReady
  case object NoWorkToBeDone

}
