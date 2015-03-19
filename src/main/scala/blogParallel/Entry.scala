package blogParallel

import akka.actor._
import com.typesafe.config.{Config, ConfigFactory}
import edu.stanford.nlp.parser.lexparser.LexicalizedParser
import java.io.File

import edu.stanford.nlp.trees.tregex.TregexPattern


//You need at least 60GB memory to run this program
object Entry{
  import SentenceSplitterMsg._
  import TimerMsg._
  import Pattern._
  import TreeReaderMsg._

  val conf: Config = ConfigFactory.load()
  val system: ActorSystem = ActorSystem("Blog", conf) //added actor logging info

  val lp = LexicalizedParser.loadModel("edu/stanford/nlp/models/lexparser/englishPCFG.ser.gz", "-MAX_ITEMS","500000") //, "-nthreads", "5"

  val futureCompiledPattern = for (pattern <- patternFuture) yield TregexPattern.compile(pattern)
  val pastCompiledPattern = for (pattern <- patternsPast) yield TregexPattern.compile(pattern)
  val presentCompiledPattern = for (pattern <- patternPresent) yield TregexPattern.compile(pattern)

  /**
   * Main method to pass in command line arguments
   * It takes 1 argument: serialId
   * @param args sbt "run --flag"
   */
  def main(args: Array[String]) {

      if (args(0) != "-serialId") {println("first flag must be '-serialId'"); System.exit(0)}
      if (args(2) != "-start") {println("first flag must be '-start'"); System.exit(0)}

      val args1 = args(1).toInt
      println(args(3))

      args(3) match {
        case "qstart" =>
          //-serialId 1 -start qstart "E:\Jason\blogFinishedCSV1.csv" "blogFinishedCSV2.csv"
          if (args.length < 5) {println("No File input warning!"); System.exit(0) }
          val files = (4 to args.length - 1).map(i => args(i)).toList
          qstart(args1, files)

        case "nstart" =>
          //-serialId 1 -start nstart "E:\Jason\blogs_multi"
          //-serialId 1 -start nstart "E:\Jason\blogs_multi" -init "E:\Jason\blogs" //will create destination file based on first fileloc
          if (args.length >= 6 && args(5) == "-init") {
              println(args.toList)
              Utility.breakDocs(args1, args(6), args(4))
          }

          val range = Utility.getBreakChunks(args(4))
          if (args1 < 0 || args1 > range) {
            println("Inserted sreialId goes beyond acceptable range: " + range)
            System.exit(0)
          }
          nstart(args1)

        case "sstart" =>
          //slow start
          //-serialId 1 -start sstart E:\Allen\Linguistics\JasonNewMturkReady.csv E:\Allen\Linguistics\JasonNewMturkProcessed.csv -column 2
          SingleThread(args(4), args(5), args(7).toInt).startSingleThread()

        case _ => println("options have to be qstart or nstart"); System.exit(0)
      }

  }

  def qstart(serialId: Int, files: List[String]): Unit = {

    val timerLoc = "E:\\Allen\\timer" + serialId + ".txt"
    val timer = system.actorOf(Props(classOf[TimerActor], timerLoc), name = "Timer")
    val error = system.actorOf(Props(classOf[ErrorActor], "E:\\Allen\\Error" + serialId + ".txt"), name = "Error")
    val filePrinter = system.actorOf(Props(classOf[FilePrintActor], new File("E:\\Allen\\Linguistics\\JasonNewMturkData" + serialId + ".csv")), name = "filePrinter")

    val tregexActor = system.actorOf(Props(classOf[TregexActor], timer, filePrinter, futureCompiledPattern, pastCompiledPattern, presentCompiledPattern), name = "TregexActor")
    val treeReader = system.actorOf(Props(classOf[TreeReaderActor], timer, tregexActor), name = "TreeReaderActor")

    Utility.printCSVHeader(new File("E:\\Allen\\Linguistics\\"  + serialId + ".csv"), List("sentence_id", "person_id", "sentence", "parsed"))

    println(files)
    val totalWork = files.map(f => Utility.countCSVLines(f)).sum
    println(totalWork)

    timer ! QMode //instantly fills up PCFG and SentenceSplitting task
    timer ! TotalTask(totalWork)

    files.map(f => treeReader ! CSVFile(f))
  }

  def nstart(serialId: Int): Unit = {
    /* Initialization Phase */

    val timerLoc = "E:\\Allen\\timer" + serialId + ".txt"

    val timer = system.actorOf(Props(classOf[TimerActor], timerLoc), name = "Timer")

    val error = system.actorOf(Props(classOf[ErrorActor], "E:\\Allen\\Error" + serialId + ".txt"), name = "Error")

    val filePrinter = system.actorOf(Props(classOf[FilePrintActor], new File("E:\\Jason\\blogFinishedCSV" + serialId + ".csv")), name = "filePrinter")

    /* Uncomment for concurrency */
//    val listOfTregexActors = (0 to 5).map(m =>
//          system.actorOf(Props(classOf[TregexActor], timer, filePrinter, futureCompiledPattern, pastCompiledPattern), name = "TregexActor" + m))
//
//    val listOfParsers = (0 to 5).map(n =>
//          system.actorOf(Props(classOf[ParserActor], timer, listOfTregexActors(n), lp), name = "ParserActor" + n))
//
//    val listOfSentenceSplitters = (0 to 5).map(j =>
//          system.actorOf(Props(classOf[SentenceSplitterActor], listOfParsers(j), timer), name = "SplitActor" + j))

    val tregexActor = system.actorOf(Props(classOf[TregexActor], timer, filePrinter, futureCompiledPattern, pastCompiledPattern, presentCompiledPattern), name = "TregexActor")
    val parserActor = system.actorOf(Props(classOf[ParserActor], timer, tregexActor, lp), name = "ParserActor")
    val sentenceActor = system.actorOf(Props(classOf[SentenceSplitterActor], parserActor, timer), name = "SplitActor")

    Utility.printCSVHeader(new File("E:\\Jason\\blogFinishedCSV"  + serialId + ".csv"), List("id", "gender", "age", "occupation", "star_sign", "date", "blog_entry", "parsed"))

    val xmlHandler = FileHandler("E:\\Jason\\blogs_multi\\" + serialId)

    /* Execution Phase */
    val listOfFiles = xmlHandler.extractXML()

    timer ! TotalTask(xmlHandler.totalNumOfEntry)
    listOfFiles.map{ ft =>
      ft._2.map{ eachEntry =>
        sentenceActor ! Post(ft._1, eachEntry._1, eachEntry._2)
      }
    }


    /* Uncomment for concurrency */
//    var entryCount = 0
//    listOfFiles.map{ ft =>
//      ft._2.map{ eachEntry =>
//        listOfSentenceSplitters(entryCount % 6) ! Post(ft._1, eachEntry._1, eachEntry._2)
//        entryCount += 1
//      }
//    }
  }


}


object Pattern {

  val patternFuture = List(
  "(VP < (VBG < going) < (S < (VP < TO)))",
  "(VP < (VBG < going) > (PP < TO))",
  "MD < will",
  "MD < ‘ll’",
  "MD < shall",
  "MD < would",
  "MD < 'd’",
  "VP < VBD << would",
  "MD < may",
  "MD < might",
  "MD < should",
  "MD < can",
  "MD < could",
  "VP < VBD << could",
  "MD < must",
  "MD < ought",
  "VB|VBD|VBG|VBN|VBP|VBZ < need|needs|needed|needing",
  "VP [ << have | << has | << had << having ] < ( S < ( VP < TO ))",
  "VP [ << supposed] < ( S < ( VP < TO ) )",
  "ADJP < ( JJ [ < unable ]) < ( S < ( VP < TO ))",
  "ADJP < ( JJ [ < able]) < ( S < ( VP < TO ))",
  "VP < ( PP < ( IN < about|of ) < ( S < ( VP <+( VP ) VBG )))",
  "SBAR < WHNP|WHADVP < (S < (VP < TO ))",
  "S < ( VP < TO < ( VP < VB ))",
  "VP [ << look | << looks | << looking]  << forward",
  "VP << want|wants|wanted|wanting|hope|hopes|hoped|hoping",
  "goal|goals|ambition",
  "upcoming|future|impending",
  "plan|plans|planned",
  "NP << need|needs",
  "tomorrow|soon|later",
  "NP << (week|weekend|month|year| ,, next)",
  "tonight",
  "NP << (week|weekend|month|year| ,, this)",
  "NP << this [ << ( weekend ,, this ) | << ( evening ,, this )]",
  "IN $ (NP < CD << hours|days|weeks|months|seasons|years)",
  "IN $ (NP < DT <<  hour|day|week|weekend|month|season|year)",
  "JJ < next $ (NN < hour|day|week|weekend|month|season|year)",
  "IN < CD << hour|day|week|weekend|month|season|year"
  )

  val patternsPast = List(
    "VBD",
  "VP [ < ( VB < have ) | < ( VBP [ < have | < 've ] ) | < ( VBZ [ < has | < 's ] ) ] < ( VP < VBN )",
  "S < (when < VBD) <<  MD < would",
  "VP [ < ( VB [ < remember | < miss | < regret | < recall | < recollect ] ) | < ( VBP [ < remember | < miss | < regret | < recall | < recollect ] ) | < ( VBZ [ < remembers | < misses | < regrets | < recalls | < recollects ] ) ] < NP",
  "forgets|forgot|forgotten|forget !.to",
  "VP [ < ( VB < thank ) | < ( VBP < thank ) | < ( VBZ < thanks ) | < ( VBG < thanking ) ] < ( PP < ( IN < for ) )",
  "VP << wish|wishes|wished|wishing",
  "NP < ( JJ|NN < past )",
  "NP < ( NP < ( NNS [ < thanks | < congratulations | < congrats | < props | < kudos | < praise ] ) ) < ( PP < ( IN < for ) )",
  "NP << regret|regrets",
  "yesterday",
  "NP < (JJ < last) < (NN < week|weekend|month|year)",
  "NP < (JJ < last) < (NNS < weeks|weekends|months|years)",
  "proud . of|former|previous",
  "ago",
  "so.far"
  ) //16

  val patternPresent = List("VBZ", "VBG", "VBP")

}