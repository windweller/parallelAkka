package blogParallel

import java.util

import FolderReadingNIO.CollectionHandler
import TwitterLinear.TwitterRegex
import akka.actor.{ActorRef, ActorLogging, Actor}
import edu.stanford.nlp.ling.{Word, HasWord}
import edu.stanford.nlp.parser.lexparser.LexicalizedParser
import edu.stanford.nlp.trees.Tree


class ParserActor(timer: ActorRef, tregexActor: ActorRef, lp: LexicalizedParser) extends Actor with ActorLogging {

  import ParserActorMsg._
  import TregexMsg._
  import TimerMsg._

  def receive = {
    case Parse(name, date, sen) =>
      import scala.collection.JavaConversions._

      val parsedSentence = PCFGparsing(name, sen)
      //break up name
      val segs = name.split("""\.""").dropRight(1).toList :+ date
      val sentenceList = CollectionHandler.buildListStringFromListHasWord(sen)

      (0 to sentenceList.length - 1).map{ n =>
        tregexActor ! Match(segs :+ sentenceList(n), parsedSentence(n))
      }

  }

  def PCFGparsing(name: String, sentences: java.util.List[java.util.List[HasWord]]): java.util.List[Tree] = {

    println("parsing starts: "+ name)

    val cleanedSentences = new java.util.ArrayList[java.util.List[HasWord]]()

    val it = sentences.listIterator()
    while (it.hasNext) { cleanedSentences.add(cleanSentence(it.next())) }

    timer ! PCFGAddOne
    lp.parseMultiple(cleanedSentences, 3)
  }

  /**
   * This very unfortunately is an state-changing funciton
   * @param sentences accept java.util.List[HasWord]
   */
  def cleanSentence(sentences: java.util.List[HasWord]): java.util.List[HasWord] = {
    import TwitterRegex._

    val cleanedSentences = new java.util.ArrayList[HasWord]()

    val it = sentences.listIterator()
    while (it.hasNext) {
      val ref = it.next()
      if (!ref.word().contains("#") && !ref.word().contains("@") && !ref.word().matches(searchPattern.toString())) {
        cleanedSentences.add(new Word(ref.word().toLowerCase))
      }
    }
    cleanedSentences
  }

}

object ParserActorMsg {
  case class Parse(name: String, date: String, sen: java.util.List[java.util.List[HasWord]])
}