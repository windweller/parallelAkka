package TwitterLinear


import com.github.tototoshi.csv.{CSVWriter, CSVReader}
import java.io.{IOException, File}
import edu.stanford.nlp.parser.lexparser.LexicalizedParser
import akka.actor.{Props, ActorSystem}
import edu.stanford.nlp.trees.Tree
import edu.stanford.nlp.trees.tregex.TregexPattern
import org.apache.commons.cli._


object Entry extends App {
  import Pattern._
  import TimerMsg._
  import FilePrintMsg._

  /**
   * Commandline options from Apache CLI
   */

  val parser = new GnuParser()
  val options = new Options()

  options.addOption("i", "inputFile", true, "read in an input csv file")
  options.addOption("h", "withHeader", false, "flag if inputFile includes header, default false")
  options.addOption("hp", "help", false, "flag it if you want an usage guide")
  options.addOption("l", "language", true, "specify the language option, default is en, if flagged, must enter value lp as well")
  options.addOption("lp", "languagePosition", true, "specify the language column position")
  options.addOption("tl", "timerLocation", true, "specify the timer file location")
  options.addOption("o", "outputFile", true, "specify the location of output file")
  options.addOption("g", "grammar", false, "specify whether to print out all the parsing grammars")
  options.addOption("s", "sentence", true, "specify the column number of the sentence/tweet")
  options.addOption("t", "test", false, "for internal testing only, don't use it")

  var cmd: CommandLine = null
  try {cmd = parser.parse(options, args)} catch {case pe: ParseException => usage(options)}

  if (cmd.hasOption("hp")) {
    usage(options)
    System.exit(0)
  }

  def usage(options: Options) {
    val formatter = new HelpFormatter()
    formatter.printHelp("FuturePast Parser", options)
  }

  var inputFileLoc, language, outputFileLoc, timerLoc = ""
  var sentence, languagePosition = 0
  val withHeader = cmd.hasOption("h")
  val withGrammar = cmd.hasOption("g")
  val withLanguage = cmd.hasOption("l")

  if (cmd.hasOption("i") && cmd.hasOption("o") && cmd.hasOption("s") && cmd.hasOption("tl")) {
    println(cmd.getOptionValue("i"))
    inputFileLoc = cmd.getOptionValue("i")
    println(cmd.getOptionValue("o"))
    outputFileLoc = cmd.getOptionValue("o")
    println(cmd.getOptionValue("tl"))
    timerLoc = cmd.getOptionValue("tl")
    println(cmd.getOptionValue("s"))
    sentence = cmd.getOptionValue("s").toInt
    if (cmd.hasOption("l")) {
      println(cmd.getOptionValue("l"))
      language = cmd.getOptionValue("l")
      if (cmd.hasOption("lp")) {
        languagePosition = cmd.getOptionValue("lp").toInt
      }else{
        System.err.println("You have to specify language column if you flagged language option")
        System.exit(1)
      }
    }

    if (!inputFileLoc.contains(".csv") || !outputFileLoc.contains(".csv")) {
      System.err.println("Input and Output file format must be .csv")
      System.exit(1)
    }

  } else if (!cmd.hasOption("t")) {
    System.err.println("You have to put in at least inputFile, outputFile, timerLoc, and sentence options to run this classifier")
    System.exit(1)
  }


  /**
   * Initialization of Akka
   */

  val system: ActorSystem = ActorSystem("Twitter")
  val timer = system.actorOf(Props(new Timer(timerLoc)), "Timer")
  val filePrinter = system.actorOf(Props[FilePrint], "filePrinter")


  /**
   * Business Logic starts
   */

  val lp = LexicalizedParser.loadModel("edu/stanford/nlp/models/lexparser/englishFactored.ser.gz", "-MAX_ITEMS","500000")

  readListByCountry(inputFileLoc,outputFileLoc)

  /**
   * Type-safe Utility function
   */
  
  def readListByCountry(inputFileLoc:String, outputFileLoc: String) {

    println("start reading!")

    val reader = CSVReader.open(new File(inputFileLoc))
    val f = new File(outputFileLoc)

    if (withHeader) { //header might cause error
      val headers = reader.allWithHeaders().head.keys.toList
      printCSVHeader(f, headers)
    }

    val rows = reader.all()

    if (withHeader) {
      timer ! TotalTask(rows.size -1)
    }else{
      timer ! TotalTask(rows.size)
    }

    for (row <- rows) {
      if (withLanguage) {
        if (row(languagePosition)==language) {
          println(row(sentence))
          val parsedSentence: Tree = PCFGparsing(row(sentence))
          val result = patternSearching(parsedSentence)
          filePrinter ! Print(row :+ parsedSentence.toString, result, f)
        }
      }else{
        println(row(sentence))
        val parsedSentence = PCFGparsing(row(sentence))
        val result = patternSearching(parsedSentence)
        filePrinter ! Print(row :+ parsedSentence.toString, result, f)
      }
    }
  }

  def printCSVHeader(f: File, headers: List[String]) {
    val writer = CSVWriter.open(f, append = false)
    if (withGrammar) {
      writer.writeRow(headers:::patternFuture:::patternsPast)
    }else{
      writer.writeRow(headers)
    }
    writer.close()
  }

  def PCFGparsing(row: String):Tree = {
    import TwitterRegex._

    println("parsing starts: "+ row)
    val wordsList = row.split("\\s+")
    val cleanedSentence = (for (word <- wordsList if !word.contains("#") && !word.contains("@")) yield word).mkString(" ").replaceAll(searchPattern.toString(), "")
    timer ! PCFGAddOne
    lp.parse(cleanedSentence.toLowerCase)
  }

  def patternSearching(tree: Tree):List[Array[Int]] = {
    val statsFuture = search(patternFuture, tree)
    val statsPast = search(patternsPast, tree)

    timer ! PatternAddOne
    List(statsFuture, statsPast)
  }

  def search(patterns: List[String], tree: Tree) = {
    val stats =  Array.fill[Int](patterns.size)(0)
    println("Entering Pattern matching")

    for (i <- 0 to patterns.size - 1) {
      val searchPattern = TregexPattern.compile(patterns(i))
      val matcher = searchPattern.matcher(tree)
      if (matcher.find()) {
        stats(i) = stats(i) + 1
      }
    }
    stats
  }

  def filePrint(row: List[String], result: List[Array[Int]], f: File) = {
    println("File writing in progress")
    val writer = CSVWriter.open(f, append = true)
    if (withGrammar) {
      writer.writeRow(row:::result.flatten.toList)
    }else{
      val future = result(0).foldLeft(0)((b, a) => b+a)
      val past = result(1).foldLeft(0)((b, a) => b+a)

      writer.writeRow(row:::List(future,past))
    }

    writer.close()
  }

  def filePrintAll(result: List[(List[String], Array[Int])], f: File) {
    println("File writing in progress")
    val writer = CSVWriter.open(f, append = true)
    result.foreach(sentence => writer.writeRow(sentence._1 ::: sentence._2.toList))
    writer.close()
  }

  def filePrintSolo(row: List[String], parsedSentence: Tree, f:File) {
    val writer = CSVWriter.open(f, append = true)

    writer.writeRow(List(row(11), row(12), row(10), row(6), row(9), row(8), row(7),
               row(2).isEmpty,parsedSentence.toString))
    println(parsedSentence.pennString())
    writer.close()
  }
}

object TwitterRegex {

  //only need to include these:
  //url, emoticon
  //or you could just import searchPattern

  val punctChars = "['\"“”‘’.?!…,:;]"
  val entity     = "&(?:amp|lt|gt|quot);"

  //URLs
  val urlStart1  = "(?:https?://|\\bwww\\.)"
  val commonTLDs = "(?:com|org|edu|gov|net|mil|aero|asia|biz|cat|coop|info|int|jobs|mobi|museum|name|pro|tel|travel|xxx)"
  val ccTLDs	 = "(?:ac|ad|ae|af|ag|ai|al|am|an|ao|aq|ar|as|at|au|aw|ax|az|ba|bb|bd|be|bf|bg|bh|bi|bj|bm|bn|bo|br|bs|bt|" +
  "bv|bw|by|bz|ca|cc|cd|cf|cg|ch|ci|ck|cl|cm|cn|co|cr|cs|cu|cv|cx|cy|cz|dd|de|dj|dk|dm|do|dz|ec|ee|eg|eh|" +
  "er|es|et|eu|fi|fj|fk|fm|fo|fr|ga|gb|gd|ge|gf|gg|gh|gi|gl|gm|gn|gp|gq|gr|gs|gt|gu|gw|gy|hk|hm|hn|hr|ht|" +
  "hu|id|ie|il|im|in|io|iq|ir|is|it|je|jm|jo|jp|ke|kg|kh|ki|km|kn|kp|kr|kw|ky|kz|la|lb|lc|li|lk|lr|ls|lt|" +
  "lu|lv|ly|ma|mc|md|me|mg|mh|mk|ml|mm|mn|mo|mp|mq|mr|ms|mt|mu|mv|mw|mx|my|mz|na|nc|ne|nf|ng|ni|nl|no|np|" +
  "nr|nu|nz|om|pa|pe|pf|pg|ph|pk|pl|pm|pn|pr|ps|pt|pw|py|qa|re|ro|rs|ru|rw|sa|sb|sc|sd|se|sg|sh|si|sj|sk|" +
  "sl|sm|sn|so|sr|ss|st|su|sv|sy|sz|tc|td|tf|tg|th|tj|tk|tl|tm|tn|to|tp|tr|tt|tv|tw|tz|ua|ug|uk|us|uy|uz|" +
  "va|vc|ve|vg|vi|vn|vu|wf|ws|ye|yt|za|zm|zw)"
  val urlStart2  = "\\b(?:[A-Za-z\\d-])+(?:\\.[A-Za-z0-9]+){0,3}\\." + "(?:"+commonTLDs+"|"+ccTLDs+")"+"(?:\\."+ccTLDs+")?(?=\\W|$)"
  val urlBody    = "(?:[^\\.\\s<>][^\\s<>]*?)?"
  val urlExtraCrapBeforeEnd = "(?:"+punctChars+"|"+entity+")+?"
  val urlEnd     = "(?:\\.\\.+|[<>]|\\s|$)"
  val url = "(?:"+urlStart1+"|"+urlStart2+")"+urlBody+"(?=(?:"+urlExtraCrapBeforeEnd+")?"+urlEnd+")"

  //  Emoticons
  val normalEyes = "(?iu)[:=]" // 8 and x are eyes but cause problems
  val wink = "[;]"
  val noseArea = "(?:|-|[^a-zA-Z0-9 ])" // doesn't get :'-(
  val happyMouths = "[D\\)\\]\\}]+"
  val sadMouths = "[\\(\\[\\{]+"
  val tongue = "[pPd3]+"
  val otherMouths = "(?:[oO]+|[/\\\\]+|[vV]+|[Ss]+|[|]+)" // remove forward slash if http://'s aren't cleaned


  // mouth repetition examples:

  val bfLeft = "(♥|0|o|°|v|\\$|t|x|;|\\u0CA0|@|ʘ|•|・|◕|\\^|¬|\\*)"
  val bfCenter = "(?:[\\.]|[_-]+)"
  val bfRight = "\\2"
  val s3 = "(?:--['\"])"
  val s4 = "(?:<|&lt;|>|&gt;)[\\._-]+(?:<|&lt;|>|&gt;)"
  val s5 = "(?:[.][_]+[.])"
  val basicface = "(?:(?i)" +bfLeft+bfCenter+bfRight+ ")|" +s3+ "|" +s4+ "|" + s5


  val eeLeft = "[＼\\\\ƪԄ\\(（<>;ヽ\\-=~\\*]+"
  val eeRight= "[\\-=\\);'\\u0022<>ʃ）/／ノﾉ丿╯σっµ~\\*]+"
  val eeSymbol = "[^A-Za-z0-9\\s\\(\\)\\*:=-]"
  val eastEmote = eeLeft + "(?:"+basicface+"|" +eeSymbol+")+" + eeRight

  val emoticon = (
    "(?:>|&gt;)?" + OR(normalEyes, wink) + OR(noseArea,"[Oo]") +
      OR(tongue+"(?=\\W|$|RT|rt|Rt)", otherMouths+"(?=\\W|$|RT|rt|Rt)", sadMouths, happyMouths),
    "(?<=(?: |^))" + OR(sadMouths,happyMouths,otherMouths) + noseArea + OR(normalEyes, wink) + "(?:<|&lt;)?",
    eastEmote.replaceFirst("2", "1"), basicface
  )

  def OR(patterns: String*) = {
    patterns.map{p => s"(?:$p)"}.mkString("|")
  }

  val searchPattern = OR(url, emoticon._1, emoticon._2, emoticon._3, emoticon._4).r
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