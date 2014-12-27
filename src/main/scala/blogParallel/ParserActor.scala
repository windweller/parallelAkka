package blogParallel

import TwitterLinear.TwitterRegex
import akka.actor.{ActorRef, ActorLogging, Actor}
import edu.stanford.nlp.ling.HasWord
import edu.stanford.nlp.parser.lexparser.LexicalizedParser
import edu.stanford.nlp.trees.Tree

import scala.collection.mutable.ListBuffer


class ParserActor(timer: ActorRef, tregexActor: ActorRef, lp: LexicalizedParser) extends Actor with ActorLogging {

  import ParserActorMsg._
  import TregexMsg._
  import TimerMsg._

  def receive = {
    case Parse(name, date, sen) =>
      import scala.collection.JavaConversions._

      val parsedSentence = PCFGparsing(name, sen)
      //break up name
      val segs = name.split("""\.""").dropRight(1).toList
      val sentenceList = constructStringFromHasWord(sen)

      (0 to sentenceList.length - 1).map{ n =>
        tregexActor ! Match(segs :+ sentenceList(n), parsedSentence(n))
      }

  }

  def constructStringFromHasWord(sentences: java.util.List[java.util.List[HasWord]]): ListBuffer[String] = {
    val it = sentences.iterator()
    val sentenceList = ListBuffer[String]()
    while (it.hasNext) {
      val sentenceSb = new StringBuilder()
      val sentence = it.next()
      for (token: HasWord <- sentence) {
        if(sentenceSb.length > 1) {
          sentenceSb.append(" ")
        }
        sentenceSb.append(token)
      }
      sentenceList += sentenceSb.toString()
    }

    sentenceList
  }

  def PCFGparsing(name: String, sentences: java.util.List[java.util.List[HasWord]]): java.util.List[Tree] = {


    println("parsing starts: "+ name)

    val it = sentences.listIterator()
    while (it.hasNext) { cleanSentence(it.next()) }

    timer ! PCFGAddOne
    lp.parseMultiple(sentences, 4)
  }

  /**
   * This very unfortunately is an state-changing funciton
   * @param sentences accept java.util.List[HasWord]
   */
  def cleanSentence(sentences: java.util.List[HasWord]): Unit = {
    import TwitterRegex._

    val it = sentences.listIterator()
    while (it.hasNext) {
      val ref = it.next()
      if (ref.word().contains("#") || ref.word().contains("@") || ref.word().matches(searchPattern.toString())) {
        sentences.remove(ref)
      }
      else {
        ref.setWord(ref.word().toLowerCase)
      }
    }
  }

}

object ParserActorMsg {
  case class Parse(name: String, date: String, sen: java.util.List[java.util.List[HasWord]])
}