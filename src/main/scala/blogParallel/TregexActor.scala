package blogParallel

import akka.actor.{ActorRef, ActorLogging, Actor}
import edu.stanford.nlp.trees.Tree
import edu.stanford.nlp.trees.tregex.TregexPattern

class TregexActor(timer: ActorRef, filePrinter: ActorRef) extends Actor with ActorLogging {

  import TregexMsg._
  import TimerMsg._
  import Pattern._
  import FilePrintMsg._

  def receive = {
    case Match(rows, sen) =>
      val result = patternSearching(sen)
      filePrinter ! Print(rows :+ sen.toString, result)
  }

  def patternSearching(tree: Tree):List[Array[Int]] = {
    val statsFuture = search(patternFuture, tree)
    val statsPast = search(patternsPast, tree)

    timer ! PatternAddOne
    List(statsFuture, statsPast)
  }

  def search(patterns: List[String], tree: Tree) = {
    val stats =  Array.fill[Int](patterns.size)(0)
    println("Entering Pattern matching")

    for (i <- 0 to patterns.size - 1) {
      val searchPattern = TregexPattern.compile(patterns(i))
      val matcher = searchPattern.matcher(tree)
      if (matcher.find()) {
        stats(i) = stats(i) + 1
      }
    }
    stats
  }
}

object TregexMsg {
  case class Match(rows: List[String], parsedSen: Tree)
}
