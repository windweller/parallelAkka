package ClassicNLP

import java.io.{FileInputStream, InputStream, PrintWriter, File}
import java.nio.charset.StandardCharsets

import TwitterLinear.TwitterRegex._
import com.clearnlp.util.UTOutput
import com.github.tototoshi.csv.{CSVWriter, CSVReader}
import clearNLP.ClearNLP
import scala.collection.mutable.ArrayBuffer
import scala.io.Source




//The semantic role labeling format requires 8 fields.
//ID: current token ID (starting at 1).
//FORM: word form.
//LEMMA: lemma.
//POS: part-of-speech tag.
//FEATS: features (different features are delimited by '|', keys and values are delimited by '=', and '_' indicates no feature).
//HEAD: head token ID.
//DEPREL: dependency label.
//SHEADS: semantic heads ('_' indicates no semantic head).


/*
  For Future vs. Past Classification
  Features that need to be there:

  PART-OF-Speech Tagging
  ROOT_WORDi
  ROOT_POSi
  ROOT_POSi-ROOT_WORDi

  Depencency Parsing
  (grab the root, if fits format VB*)
  WORDr
  POSr
  LEMMAr
  POSr-LEMMAr

  Semantic Role Labeling
  LEMMAadv (AM-ADV)
  LEMMAadv-WORDr
  LEMMAadv-POSr
  TMP

 */

/*
  Feature Vector

  Have a label array with only two labels: Future vs Past
  Q1. Assign label for each word? How to handle a long conjunctive sentence that contains both future and past? How to break it up?
  Q2. Semantic Labeling and Dependency parsing (root word) and Semantic Role Labeling (AM-ADV, AM-TMP) are based on sentences, so return to Q1.

*/


object Entry extends App{

//  sanitizeTweets("E:\\Allen\\TwitterProject\\ClearNLPConf\\twitter2302TestDataCleaned.txt")
//  val cnlp = new ClearNLP("general-en", "E:\\Allen\\TwitterProject\\ClearNLPConf\\twitter2302TestDataCleaned2273.txt", "E:\\Allen\\TwitterProject\\ClearNLPConf\\twitter2302TestDataOutput.txt")

  //labeling: future: 1, not future: 0


  val labelCount: Map[String, Int] = Map("Future" -> 0, "Not Future" -> 0)
  val humanLabels = (for(line <- Source.fromFile("E:\\Allen\\TwitterProject\\ClearNLPConf\\tweetLabeling.txt").getLines()) yield line).toList
  val features:ArrayBuffer[String] = ArrayBuffer()

  var sentencesWithFeature:ArrayBuffer[Map[String, List[Int]]] = null

  var currentSentence: ArrayBuffer[List[String]] = ArrayBuffer(List())
  var currentIndex = 0

  startVectorize()

  def startVectorize() = {
    val input = new FileInputStream(new File("E:\\Allen\\TwitterProject\\ClearNLPConf\\twitter2302TestDataOutput.txt"))
    val file = toSource(input)
    val lines = for(line <- file.getLines()) {

      //seperator between sentences, accumulate them
      if(line.nonEmpty) {
        currentSentence += line.split("\t").toList
      }else{
        //Convert to vector when all sentences are processed
        //if no feature, then empty list
        val sentenceFeatures = convertFeature(currentSentence.drop(1)) //drop first empty List()s

        sentenceFeatures match {
          case Some(feature) =>
            if (sentencesWithFeature == null) sentencesWithFeature = ArrayBuffer(Map(humanLabels(currentIndex) -> feature))
            else sentencesWithFeature += Map(humanLabels(currentIndex) -> feature)
          case None =>
            if (sentencesWithFeature == null) sentencesWithFeature = ArrayBuffer(Map(humanLabels(currentIndex) -> List()))
            else sentencesWithFeature += Map(humanLabels(currentIndex) -> List())
        }

        currentIndex += 1
        currentSentence.clear()
      }
    }
  }

  def sanitizeTweets(outputFile: String) = {
    val input = new FileInputStream(new File("E:\\Allen\\TwitterProject\\ClearNLPConf\\twitter2302TestData.txt"))

    val file = toSource(input)

    var rows: ArrayBuffer[String] = ArrayBuffer()

    for(row <-file.getLines()) {rows += row}

    println(rows.length)

    var cleanedSentences: ArrayBuffer[String] = ArrayBuffer()

    for (row <- rows) {
      val wordsList = row.split("\\s+")
      val cleanedSentence = (for (word <- wordsList if !word.contains("#") && !word.contains("@")) yield word).mkString(" ").replaceAll(searchPattern.toString(), "")
      cleanedSentences += cleanedSentence
    }

    val writer = new PrintWriter(new File(outputFile))
    var counter = 0
    var finalCounter = 0
    cleanedSentences.map{s => println(s); counter+=1; if (s != " " && s != "\r\n" && s != "") {writer.write(s + "\r\n"); finalCounter += 1}}

    println(counter)
    println(finalCounter) //2273 sentences after cleaning (sentences taken out only contain url or emoticons)

  }

  //Scala way to avoid mixed input warning/errors
  //http://stackoverflow.com/questions/13625024/how-to-read-a-text-file-with-mixed-encodings-in-scala-or-java
  def toSource(inputStream:InputStream): scala.io.BufferedSource = {
    import java.nio.charset.Charset
    import java.nio.charset.CodingErrorAction
    val decoder = Charset.forName("UTF-8").newDecoder()
    decoder.onMalformedInput(CodingErrorAction.IGNORE)
    scala.io.Source.fromInputStream(inputStream)(decoder)
  }


  //when every line is processed, we have a full list of features and every sentence with corresponding features
  //then we traverse all arrays converting them to vectors

//  println(sentencesWithFeature.length) //520 in total; with the parsed file, we only have 520 sentences; test by using: val lines = for(line <- scala.io.Source.fromFile("E:\\Allen\\TwitterProject\\ClearNLPConf\\mTurk528SentencesSummerOutput.txt").getLines() if line == "") yield line

  //  println(features.length) // 626 features in total
  sentencesWithFeature.map(senten => convertToVector(senten))

  // 0 means not future; 1 means future!
  // Map(0 -> List(0, 1, 2, 3, 4)), Map(1 -> List(622, 11, 17, 83, 623))

  def convertToVector(sentence: Map[String, List[Int]]) = {
    println("File writing in progress")

    val featureVector = ArrayBuffer.fill(features.length)(0)

    //update the feature vector
    sentence.toList(0)._2.map(num => featureVector(num) = 1)

    val writer = CSVWriter.open("E:\\Allen\\TwitterProject\\ClearNLPConf\\twitter2273Vector.csv", append=true)
    writer.writeRow(sentence.toList(0)._1 ++ featureVector.toList)

    featureVector.clear() //maybe unnecessary, but just to make sure
  }

  /**
   * Grabbing the POS phase
   */
  def convertFeature(currentSentence: ArrayBuffer[List[String]]): Option[List[Int]] = {

    //POS phase
    val resultPOS = for (wordLabels <- currentSentence if wordLabels(6) == "root" && wordLabels(3).contains("VB")) yield wordLabels
    if (resultPOS.nonEmpty) {
      val wordR = "WORDr=" + resultPOS(0)(1)
      val posR = "POSr=" + resultPOS(0)(3)
      val lemmaR = "LEMMAr=" + resultPOS(0)(2)
      val lemmaR_posR = "lemmaR_posR=" + resultPOS(0)(2) + " + " + resultPOS(0)(3)
      val wordR_posR = "wordR_posR=" + resultPOS(0)(1) + " + " + resultPOS(0)(3)
      //this returns sentenceFeature
      val sentencePOSFeature = List(wordR, posR, lemmaR, lemmaR_posR, wordR_posR).map(f => checkFeature(f))

      //Semantic Role labeling phase
      val resultADV = for (wordLabels <- currentSentence if wordLabels(7) == "AM-ADV") yield wordLabels
      val resultTMP = for (wordLabels <- currentSentence if wordLabels(7) == "AM-TMP") yield wordLabels

      var sentenceADVFeature:List[Int] = List()
      if (resultADV.nonEmpty) {
        val lemmaADV = "LEMMAadv=" + resultADV(0)(2)
        val lemmaADV_wordR = "LEMMAadv_WORDr=" + resultADV(0)(2) + " + " + resultPOS(0)(1)
        val lemmaADV_posR = "LEMMAadv_POSr=" + resultADV(0)(2) + " + " + resultPOS(0)(3)

        sentenceADVFeature = List(lemmaADV, lemmaADV_posR, lemmaADV_wordR).map(f => checkFeature(f))
      }

      var sentenceTMPFeature:List[Int] = List()
      if (resultTMP.nonEmpty) {
        val lemmaTMP = "LEMMAtmp=" + resultTMP(0)(2)
        sentenceTMPFeature = List(lemmaTMP).map(f => checkFeature(f))
      }

      Some(sentencePOSFeature++sentenceADVFeature++sentenceTMPFeature)
    }else{
      None
    }
  }

  /**
   * Check if the feature is included
   * If not, add to it.
   * return a modified sentenceFeature ArrayBuffer
   * @param featureString
   */
  def checkFeature(featureString: String): Int = {
    if (features.contains(featureString)) {
      features.indexOf(featureString)
    }else{
      features += featureString
      features.length - 1
    }
  }


}
