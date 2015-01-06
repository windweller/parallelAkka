package blogParallel

import java.io.{StringReader, File}

import FolderReadingNIO.CollectionHandler
import Pattern._
import blogParallel.FilePrintMsg.Print
import blogParallel.TimerMsg._
import TwitterLinear.TwitterRegex
import akka.actor.{Props, ActorSystem}
import blogParallel.ParserActorMsg.Parse
import blogParallel.SentenceSplitterMsg.Post
import blogParallel.TregexMsg.Match
import com.typesafe.config.{ConfigFactory, Config}
import edu.stanford.nlp.ling.{Word, HasWord}
import edu.stanford.nlp.parser.lexparser.LexicalizedParser
import edu.stanford.nlp.process.DocumentPreprocessor
import edu.stanford.nlp.trees.Tree
import edu.stanford.nlp.trees.tregex.TregexPattern

case class SingleThread(serialId: Int) {

  val timerLoc = "E:\\Allen\\timer" + serialId + ".txt"

  val conf: Config = ConfigFactory.load()
  val system: ActorSystem = ActorSystem("Blog", conf) //added actor logging info

  val timer = system.actorOf(Props(classOf[TimerActor], timerLoc), name = "Timer")

  val error = system.actorOf(Props(classOf[ErrorActor], "E:\\Allen\\Error" + serialId + ".txt"), name = "Error")

  val filePrinter = system.actorOf(Props(classOf[FilePrintActor], new File("E:\\Jason\\blogFinishedCSV" + serialId + ".csv")), name = "filePrinter")

  val lp = LexicalizedParser.loadModel("edu/stanford/nlp/models/lexparser/englishPCFG.ser.gz", "-MAX_ITEMS","500000") //, "-nthreads", "5"

  val futureCompiledPattern = for (pattern <- patternFuture) yield TregexPattern.compile(pattern)
  val pastCompiledPattern = for (pattern <- patternsPast) yield TregexPattern.compile(pattern)

  startSingleThread()

  def startSingleThread(): Unit = {
    Utility.printCSVHeader(new File("E:\\Jason\\blogFinishedCSV"  + serialId + ".csv"), List("id", "gender", "age", "occupation", "star_sign", "date", "blog_entry", "parsed"))

    val xmlHandler = FileHandler("E:\\Jason\\blogs_multi\\" + serialId)

    /* Execution Phase */
    val listOfFiles = xmlHandler.extractXML()

    timer ! TotalTask(xmlHandler.totalNumOfEntry)

    listOfFiles.map{ ft =>
      ft._2.map{ eachEntry =>
         val parse = sentenceSplitterFunc(Post(ft._1, eachEntry._1, eachEntry._2))
         parseFunc(parse)
      }
    }
  }

  def sentenceSplitterFunc(post: Post): Parse = {
    val processorIterator = new DocumentPreprocessor(new StringReader(post.rawPost))
    val sentences = CollectionHandler.construct2dList()

    val it = processorIterator.iterator()

    while (it.hasNext) {
      sentences.add(it.next())
    }

    timer ! SentenceSplitAddOne

    Parse(post.fileName, post.date, sentences)
  }

  def parseFunc(parse: Parse): Unit = {
    import scala.collection.JavaConversions._
    val parsedSentence = PCFGparsing(parse.name, parse.sen)
    val segs = parse.name.split("""\.""").dropRight(1).toList :+ parse.date
    val sentenceList = CollectionHandler.buildListStringFromListHasWord(parse.sen)

    timer ! UpdatePatternTask(sentenceList.length)

    (0 to sentenceList.length - 1).map{ n =>
      tregexMatchFunc(Match(segs :+ sentenceList(n), parsedSentence(n)))
    }
  }

  def tregexMatchFunc(tmatch: Match): Unit = {
    println("Entering Pattern matching: " + tmatch.rows(0))
    val result = patternSearching(tmatch.parsedSen)
    filePrinter ! Print(tmatch.rows :+ tmatch.parsedSen.toString, result)
  }

  def PCFGparsing(name: String, sentences: java.util.List[java.util.List[HasWord]]): java.util.List[Tree] = {

    println("parsing starts: "+ name)

    var cleanedSentences = new java.util.ArrayList[java.util.List[HasWord]]()

    val it = sentences.listIterator()
    while (it.hasNext) { cleanedSentences.add(cleanSentence(it.next())) }

    timer ! PCFGAddOne
    val result = lp.parseMultiple(cleanedSentences, 4)

    cleanedSentences = null
    result
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

  def patternSearching(tree: Tree):List[Array[Int]] = {
    val statsFuture = search(futureCompiledPattern, tree)
    val statsPast = search(pastCompiledPattern, tree)

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
