package blogParallel

import java.io.{PrintWriter, StringReader}

import akka.actor.{ActorRef, ActorLogging, Actor}
import edu.stanford.nlp.ling.StringLabelFactory
import edu.stanford.nlp.trees.{LabeledScoredTreeFactory, PennTreeReader, TreeReader, Tree}
import edu.stanford.nlp.trees.tregex.TregexPattern

class TregexActor(timer: ActorRef, filePrinter: ActorRef,
                  future: List[TregexPattern], past: List[TregexPattern], present: List[TregexPattern]) extends Actor with ActorLogging {

  import TregexMsg._
  import TimerMsg._
  import Pattern._
  import FilePrintMsg._

  def receive = {
    case Match(rows, sen) =>
      println("Entering Pattern matching: " + rows(0))
      val result = patternSearching(sen, rows(0), sen.toString)
      filePrinter ! Print(rows :+ sen.toString, result)

    case StringMatch(rows, sen) =>
      println("Entering Pattern matching: " + rows(0))
      val result = patternSearching(Tree.valueOf(sen), rows(0), sen)
      filePrinter ! Print(rows :+ sen.toString, result)
  }

  def patternSearching(tree: Tree, treeId: String, rawSen: String):List[Array[Int]] = {
    val statsFuture = search(future, tree, treeId, rawSen)
    val statsPast = search(past, tree, treeId, rawSen)
    val statsPresent = search(present, tree, treeId, rawSen)

    List(statsFuture, statsPast, statsPresent)
  }

  def search(patterns: List[TregexPattern], tree: Tree, treeId: String, rawSen: String) = {
    val stats =  Array.fill[Int](patterns.size)(0)

    for (i <- 0 to patterns.size - 1) {

      try {
        val matcher = patterns(i).matcher(tree)
        if (matcher.find()) {
          stats(i) = stats(i) + 1
        }
      } catch {
        case e: NullPointerException =>
          //this happens when a tree is malformed
          log.info("NULL Pointer with " + treeId + " : " + rawSen)
          //we will not add any number to stats, just return it as is
      }

      timer ! PatternAddOne
    }

    stats
  }

  //This is how Tregex is doing; we are using Tree.valueOf(sen) to replace that
  def buildTree(rawString: String): Tree = {
    val r: TreeReader = new PennTreeReader(new StringReader(rawString),
      new LabeledScoredTreeFactory(new StringLabelFactory))

    r.readTree
  }
}

object TregexMsg {
  case class Match(rows: List[String], parsedSen: Tree)
  case class StringMatch(rows: Seq[String], parsedSen: String)
}
