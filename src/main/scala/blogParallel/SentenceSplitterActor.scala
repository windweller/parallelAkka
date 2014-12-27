package blogParallel

import java.io.StringReader

import akka.actor.{ActorRef, ActorLogging, Actor}
import edu.stanford.nlp.ling.HasWord
import edu.stanford.nlp.parser.lexparser.LexicalizedParser
import edu.stanford.nlp.process.DocumentPreprocessor
import FolderReadingNIO.CollectionHandler

import scala.collection.mutable.ListBuffer

class SentenceSplitterActor(parserActor: ActorRef, timer: ActorRef,
                            tregexActor: ActorRef, lp: LexicalizedParser) extends Actor with ActorLogging {

  import SentenceSplitterMsg._

  def receive = {
    case Post(fileName, date, rawPost) =>

      val processorIterator = new DocumentPreprocessor(new StringReader(rawPost))
      val sentences = CollectionHandler.construct2dList()

      val it = processorIterator.iterator()

      while (it.hasNext) {
        sentences.add(it.next())
      }

  }

}

object SentenceSplitterMsg {
  case class Post(fileName: String, date: String, rawPost: String)
}