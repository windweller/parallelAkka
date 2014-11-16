package AnxietyClassificationPre

import java.io.File

import com.github.tototoshi.csv.{CSVWriter, CSVReader}
import Util._


object Entry extends App{
    /**
     * Steps:
     * 1. In file either "Pilot2_parse_flags_out2_compressed1.xlsx"
     * Get user ID (first row) and associate with anxiety score
     *
     * column 109 - 128 (index starts from 0)
     *
     * 129 - 148 is the form B
     *
     * Score interpretation. Range of scores for each subtest is
      20–80, the higher score indicating greater anxiety. A cut
      point of 39–40 has been suggested to detect clinically
      significant symptoms for the S-Anxiety scale (9,10); however,
      other studies have suggested a higher cut score of
      54–55 for older adults (11). Normative values are available
      in the manual (12) for adults, college students, and psychiatric
      samples. To this author’s knowledge, no cut score
     *
     * 2. Use UserID to match up with sentences in "Pilot2_parse_flags_out2.tab" file
     * match column B (SNum) with userID, one ID normally has multiple matched sentences
     * save sentences and column A (SentID) into a List[String]
     *
     * 3. Print out CSV file with the following format:
     * SentID (because it's unique) sentence (as feature) AnxietyScore (Label)
     */

    //This is for survey evaluation score
    val e = List(1,2,3,4)
    val eB = List(4,3,2,1)

    val pilot2_parse_flags = "E:\\Allen\\TwitterProject\\Pilot2\\Pilot2_parse_flags_out2_compressed1.csv"
    val sentenceFile = "E:\\Allen\\TwitterProject\\Pilot2\\Pilot2_parse_flags_out.tab"
    val outputFileLocale = "E:\\Allen\\TwitterProject\\Pilot2\\AnxietyTrainingCorpus.txt"

    //program runs
    val updatedMTurkers = getSentences(getAnxietyScore)
    saveToMalletFormatBySentence(updatedMTurkers)

    def getAnxietyScore = {
      println("Start Anxiety Score...")
      val reader = CSVReader.open(new File(pilot2_parse_flags))

      val rows = reader.all().drop(1) //took out the first header row

      val mTurkers = for (row <- rows) yield {
        val anxietyS = eB(row(108)-1) + e(row(109)-1) + e(row(110)-1) + e(row(111)-1) + eB(row(112)-1) +
         e(row(113)-1) + e(row(114)-1) + eB(row(115)-1) + e(row(116)-1) + eB(row(117)-1) +
         eB(row(118)-1) + e(row(119)-1) + e(row(120)-1) + e(row(121)-1) + eB(row(122)-1) +
         eB(row(123)-1) + e(row(124)-1) + e(row(125)-1) + eB(row(126)-1) + eB(row(127)-1)

        val isAnxiety = if (anxietyS >= 40) "Anxiety" else "NAnxiety"

        MTurker(row(0), row(1) , None, None, Some(anxietyS), Some(isAnxiety))
      }

      mTurkers
    }

    /**
     * Filter sentence with regex experession and
     * take out url and hashtag
     * maybe one version without filtering out them
     * @param mTurkers
     * @return
     */
    def getSentences(mTurkers: List[MTurker]): List[MTurker] = {

      import TwitterRegex._

      println("Start getting sentences...")

      val lines = for ( line <- scala.io.Source.fromFile(sentenceFile).getLines()) yield {
        line.split("\t")
      }.toList
      val rows = lines.toList.drop(1) //took out the first header row

      //yield a tuple of "SentID", "SNum" (user ID), sentence text
      val collections = for (row <- rows) yield {(row(0), row(1), row(5))}

      for (mturker <- mTurkers) yield {
        val matched = collections.filter(c => c._2 == mturker.userID)

        mturker.copy(sentID = Some(matched.map(c => c._1)),sentences = Some(matched.map{c =>
          val wordsList = c._3.split("\\s+")
          (for (word <- wordsList if !word.contains("#") && !word.contains("@")) yield word)
            .mkString(" ")
            .replaceAll(searchPattern.toString(), "")
            .toLowerCase
        }))
      }
    }

    /**
     *   In general, use CsvIterator three groups: uniGroup, targetGroup, dataGroup
     *   as [Name] [label] [data]
     *   here we produce
     *   sentID anxiety/NAnxiety Sentence
     *
     *   comma spliced
     */
    def saveToMalletFormatBySentence(mTurkers: List[MTurker]) {
      val writer = CSVWriter.open(new File(outputFileLocale))

      for (
        mturker <- mTurkers;
        sentenceId <- mturker.sentID.get;
        sentence <- mturker.sentences.get
      ) {
        writer.writeRow(List(sentenceId, mturker.anxietyLabel.get, sentence))
      }
    }

}

case class MTurker(userID: String, sentNum: Int, sentID: Option[List[String]],
                   sentences: Option[List[String]], anxietyS: Option[Int], anxietyLabel: Option[String])

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