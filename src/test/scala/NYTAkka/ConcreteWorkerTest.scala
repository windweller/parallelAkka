package NYTAkka

import akka.actor.{ActorPath, Props, ActorSystem}
import akka.testkit.{ImplicitSender, TestKit}
import org.scalatest.{WordSpecLike, MustMatchers, BeforeAndAfterAll}

/**
 * Created by anie on 3/13/2015.
 */
class ConcreteWorkerTest extends TestKit(ActorSystem("WorkerSpec"))
                                    with ImplicitSender
                                    with WordSpecLike
                                    with BeforeAndAfterAll
                                    with MustMatchers {

  override def afterAll() { system.shutdown() }

  def worker(name: String) = system.actorOf(Props(
    new ConcreteWorker(ActorPath.fromString("akka://%s/user/%s".format(system.name, name))
  )))

  "Worker" should {
    "work" in {
      //Spin up master
      val m = system.actorOf(Props[Master], "master")

      //create three workers
      val w1 = worker("master")
      val w2 = worker("master")
      val w3 = worker("master")

      //Send some work to the master
      m ! "Hithere"
      m ! "what up"
      m ! "msg 3"
      m ! "hello"

      //get all back
      expectMsgAllOf("Hithere", "what up", "msg 3", "hello")
    }
  }
}
