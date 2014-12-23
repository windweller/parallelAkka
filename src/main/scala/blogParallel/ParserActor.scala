package blogParallel

import TwitterLinear.TwitterRegex
import akka.actor.{ActorRef, ActorLogging, Actor}
import edu.stanford.nlp.parser.lexparser.LexicalizedParser
import edu.stanford.nlp.trees.Tree


class ParserActor(timer: ActorRef, tregexActor: ActorRef, lp: LexicalizedParser) extends Actor with ActorLogging {

  import ParserActorMsg._
  import TregexMsg._
  import TimerMsg._

  def receive = {
    case Parse(name, sen) =>
      val parsedSentence = PCFGparsing(sen)
      //break up name
      val segs = name.split("""\.""")
      //start to call TregexActor
      tregexActor ! Match(segs.dropRight(1).toList :+ sen, parsedSentence)
  }

  def PCFGparsing(row: String):Tree = {
    import TwitterRegex._

    println("parsing starts: "+ row)

    val wordsList = row.split("\\s+")
    val cleanedSentence = (for (word <- wordsList if !word.contains("#") && !word.contains("@")) yield word).mkString(" ").replaceAll(searchPattern.toString(), "")
    timer ! PCFGAddOne
    lp.parse(cleanedSentence.toLowerCase)
  }

}

object ParserActorMsg {
  case class Parse(name: String, sen: String)
}