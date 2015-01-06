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

  /**
   * Main method to pass in command line arguments
   * It takes 1 argument: serialId
   * @param args sbt "run --flag"
   */
  def main(args: Array[String]) {

//    println(args.toList)
//    Utility.breakDocs(6, "E:\\Jason\\blogs", "E:\\Jason\\blogs_multi")

      if (args(0) != "-serialId") {println("first flag must be '-serialId'"); System.exit(0)}

      val range = Utility.getBreakChunks("E:\\Jason\\blogs_multi")
      val args1 = args(1).toInt

      if (args1 < 0 || args1 > range) {
        println("Inserted sreialId goes beyond acceptable range: " + range)
        System.exit(0)
      }

//      SingleThread(args1).startSingleThread()
      start(args1)
  }

  def start(serialId: Int): Unit = {
    /* Initialization Phase */

    val timerLoc = "E:\\Allen\\timer" + serialId + ".txt"

    val conf: Config = ConfigFactory.load()
    val system: ActorSystem = ActorSystem("Blog", conf) //added actor logging info

    val timer = system.actorOf(Props(classOf[TimerActor], timerLoc), name = "Timer")

    val error = system.actorOf(Props(classOf[ErrorActor], "E:\\Allen\\Error" + serialId + ".txt"), name = "Error")

    val filePrinter = system.actorOf(Props(classOf[FilePrintActor], new File("E:\\Jason\\blogFinishedCSV" + serialId + ".csv")), name = "filePrinter")

    val lp = LexicalizedParser.loadModel("edu/stanford/nlp/models/lexparser/englishPCFG.ser.gz", "-MAX_ITEMS","500000") //, "-nthreads", "5"

    val futureCompiledPattern = for (pattern <- patternFuture) yield TregexPattern.compile(pattern)
    val pastCompiledPattern = for (pattern <- patternsPast) yield TregexPattern.compile(pattern)

    /* Uncomment for concurrency */
//    val listOfTregexActors = (0 to 5).map(m =>
//          system.actorOf(Props(classOf[TregexActor], timer, filePrinter, futureCompiledPattern, pastCompiledPattern), name = "TregexActor" + m))
//
//    val listOfParsers = (0 to 5).map(n =>
//          system.actorOf(Props(classOf[ParserActor], timer, listOfTregexActors(n), lp), name = "ParserActor" + n))
//
//    val listOfSentenceSplitters = (0 to 5).map(j =>
//          system.actorOf(Props(classOf[SentenceSplitterActor], listOfParsers(j), timer), name = "SplitActor" + j))

    val tregexActor = system.actorOf(Props(classOf[TregexActor], timer, filePrinter, futureCompiledPattern, pastCompiledPattern), name = "TregexActor")
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

  val patternFuture = List("(PP < (IN < about) < (S < (VP <+(VP) VBG)))",
    "(PP < (IN < of) < (S < (VP <+(VP) VBG)))",
    "(VP < (VBG < going) < (S < (VP < TO)))",
    " (VP < (VBG < going) < (PP < TO))",
    "VP < VB < (PP < (IN < for))",
    "VP < VP < VB|VBG < (PP < (IN < after))",
    "VP < NN < (S < (VP < TO))",
    "VP < MD < (VP < VB)",
    "VP < (MD < will)",
    "VP < (MD < would)",
    "VP < (MD < may)",
    "VP < (MD < might)",
    "VP < (MD < can)",
    "VP < (MD < could)",
    "VP < (MD < shall)",
    "VP < (MD < should)",
    "VP < (VBD|VBP < had|have) < (S < (VP < TO))",
    "SBAR < WHNP < (S < (VP <  TO))",
    "SBAR < WHADVP < (S < (VP <  TO))",
    "SBAR < WHNP < (S < (VP < (S < (VP < TO))))",
    "SBAR < WHADVP < (S < (S < (VP < TO)))",
    "S  <+(!S) tomorrow",
    "S <+(!S) will",
    "S <+(!S) would|should|might",
    "S <+(!S) could|may",
    "SBAR < IN < (S < (VP <  TO))",
    "VP << look << ADVP << forward",
    "IN $ (NP < CD << hours|days|weeks|months|seasons|years)",
    "IN $ (NP < DT <<  hour|day|week|weekend|month|season|year)",
    "JJ < next $ (NN < hour|day|week|weekend|month|season|year)",
    "IN < CD << hour|day|week|weekend|month|season|year",
    "VP < VBP|VBG < (ADVP << forward)",
    "want|wanted|wanting|hope|hoped|hoping|wish|wished|wishing",
    "try",
    "leaving",
    "goal|goals|ambition",
    "VBZ|VBP| < need|needs",
    "VP < (VB < complete)",
    "upcoming|future",
    "VB|VBP < plan",
    "consider|considered|considering|decide|decided|worry|worried|worrying",
    "tomorrow|tonight|soon|later|impending",
    "NP << this [ << ( weekend ,, this ) | << ( evening ,, this ) ] [ !<< past & !<< last & !<< previous ]",
    "VP [ << need | << needs | << needed | << needing ] < ( S < ( VP < TO < ( VP < VB ) ) )",
    "VP [ << try | << tries | << tried | << trying ] < ( S < ( VP < TO < ( VP < ( VB [ < do | < make | < reach | < finish | < complete | < start | < begin | < get ] ) ) ) ) ",
    "VP [ << think | << thinks | << thought | << thinking ] < ( PP < ( IN [ < about | < of ] ) < ( S < ( VP < VBG ) ) ) ",
    "VP < ( VBG [ !< trying & !< causing ] ) < ( S < ( VP < TO < ( VP < VB ) ) )",
    "VP < ( VBN [ < supposed | < told | < asked ] ) < ( S < ( VP < TO < ( VP < VB ) ) )",
    "VP < ( MD < must ) << ( VP < VB )",
    "SBAR < WHNP < ( S < ( VP < VBZ < ( VP < ( VBG [ < going | < coming | < leaving | < approaching ] ) ) ) )",
    "ADJP < ( JJ [ < able | < unable ] ) < ( S < ( VP < TO < ( VP < ( VB [ < do | < make | < reach | < finish | < complete | < start | < begin | < get ] ) ) ) )",
    "VP < ( VBG < going ) < ( PP < ( IN < on ) < NP )",
    "VP [ << want | << wants | << wanted | << wanting ] < ( S < ( VP < TO < ( VP < VB ) ) )"
  ) //53

  val patternsPast = List(
    "VBD",
    "VP [ < ( VB < have ) | < ( VBP [ < have | < 've ] ) | < ( VBZ [ < has | < 's ] ) ] < ( VP < VBN )",
    "VP [ < ( VB [ < remember | < miss | < regret | < recall | < recollect ] ) | < ( VBP [ < remember | < miss | < regret | < recall | < recollect ] ) | < ( VBZ [ < remembers | < misses | < regrets | < recalls | < recollects ] ) ] < ( VP < VBG )",
    "VP [ < ( VB [ < remember | < miss | < regret | < recall | < recollect ] ) | < ( VBP [ < remember | < miss | < regret | < recall | < recollect ] ) | < ( VBZ [ < remembers | < misses | < regrets | < recalls | < recollects ] ) ] < NP",
    "yesterday",
    "past",
    "forget|forgets|forgot|forgotten",
    "VP < ( VBZ [ < says | < tells | < asks | < writes | < comments | < explains | < reports | < warns | < suggests | < states | < promises | < complains | < agrees | < admits ] ) < SBAR",
    "VP [ < ( VB < thank ) | < ( VBP < thank ) | < ( VBZ < thanks ) | < ( VBG < thanking ) ] < ( PP < ( IN < for ) )",
    "NP < ( NP < ( NNS [ < thanks | < congratulations | < congrats | < props | < kudos | < praise ] ) ) < ( PP < ( IN < for ) )",
    /*can't parse*/ //"ADVP < ( RB < so ) < ( RB < far ) | CONJP < ( RB < so ) < ( RB < far )",
    "NP < ( DT < every ) < ( NN < time )",
    "( MD !.. have ) .. done",
    "regret|regrets",
    "proud . of",
    "again",
    "always !,, MD !.. MD"
  ) //16

}