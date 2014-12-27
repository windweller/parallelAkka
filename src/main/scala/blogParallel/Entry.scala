package blogParallel

import akka.actor._
import com.github.tototoshi.csv.CSVWriter
import edu.stanford.nlp.parser.lexparser.LexicalizedParser
import java.io.File
import XMLHandler._

import scala.io.Source

//You need at least 60GB memory to run this program
object Entry extends App{
  import Pattern._
  import ParserActorMsg._
  import ErrorMsg._
  import TimerMsg._

  /* Initialization Phase */

  val timerLoc = "E:\\Allen\\timer.txt"

  val system: ActorSystem = ActorSystem("Twitter")
  val timer = system.actorOf(Props(new TimerActor(timerLoc)), "Timer")
  val error = system.actorOf(Props(new ErrorActor("E:\\Allen\\Error.txt")), "Error")
  val filePrinter = system.actorOf(Props(new FilePrintActor(new File("E:\\Jason\\blogFinishedCSV.csv"))), "filePrinter")

  val lp = LexicalizedParser.loadModel("edu/stanford/nlp/models/lexparser/englishPCFG.ser.gz", "-MAX_ITEMS","500000") //, "-nthreads", "5"

  val listOfTregexActors = (0 to 5).map(m => system.actorOf(Props(new TregexActor(timer, filePrinter)), "TregexActor" + m))
  val listOfParsers = (0 to 5).map(n => system.actorOf(Props(new ParserActor(timer, listOfTregexActors(n), lp)), "ParserActor" + n))

  timer ! TotalTask(3386)

  /* Execution Phase */

  Utility.printCSVHeader(new File("E:\\Jason\\blogFinishedCSV.csv"), List("id", "gender", "age", "occupation", "star_sign", "date", "blog_entry", "parsed"))
  val listOfFiles = extractXML("E:\\Jason\\blogs_test")



  //  val file = scala.io.Source.fromFile("E:\\Jason\\blogs_sample.tab", "UTF-8") //300mb in memory now



  //legacy code, delete it
//  var lineCount = 0
//  for (line <- Source.fromFile("E:\\Jason\\blogs_sample.tab", "UTF-8").getLines()) {
//      val segs = line.split("\t")
//      if (segs.length > 2) error ! Warning("this line contains tab space inside sentences: " + segs(0))
//      listOfParsers(lineCount % 6) ! Parse(segs(0), segs(1))
//      lineCount += 1
//  }


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