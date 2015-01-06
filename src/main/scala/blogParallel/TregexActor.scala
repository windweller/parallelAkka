package blogParallel

import akka.actor.{ActorRef, ActorLogging, Actor}
import edu.stanford.nlp.trees.Tree
import edu.stanford.nlp.trees.tregex.TregexPattern

class TregexActor(timer: ActorRef, filePrinter: ActorRef,
                  future: List[TregexPattern], past: List[TregexPattern]) extends Actor with ActorLogging {

  import TregexMsg._
  import TimerMsg._
  import Pattern._
  import FilePrintMsg._

  def receive = {
    case Match(rows, sen) =>
      println("Entering Pattern matching: " + rows(0))
      val result = patternSearching(sen)
      filePrinter ! Print(rows :+ sen.toString, result)
  }

  def patternSearching(tree: Tree):List[Array[Int]] = {
    val statsFuture = search(future, tree)
    val statsPast = search(past, tree)

    List(statsFuture, statsPast)
  }

  def search(patterns: List[TregexPattern], tree: Tree) = {
    val stats =  Array.fill[Int](patterns.size)(0)

    for (i <- 0 to patterns.size - 1) {
      val matcher = patterns(i).matcher(tree)
      if (matcher.find()) {
        stats(i) = stats(i) + 1
      }
      timer ! PatternAddOne
    }
    stats
  }
}

object TregexMsg {
  case class Match(rows: List[String], parsedSen: Tree)
}
