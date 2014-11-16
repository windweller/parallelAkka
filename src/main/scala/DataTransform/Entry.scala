package DataTransform

import java.nio.file.{Files, Paths}

import com.github.tototoshi.csv.{CSVWriter, CSVReader}
import java.io.{PrintWriter, File}
import edu.stanford.nlp.parser.lexparser.LexicalizedParser
import scalax.io._
import java.nio.file.StandardOpenOption._

object Entry extends App {


  val future = Evaluation.generateXMLTree("E:\\Allen\\Linguistics\\futureOrderedSubtrees.xml")
//  val notFuture = Evaluation.generateXMLTree("E:\\Allen\\Linguistics\\notFutureOrderedSubtrees.xml")

//  val combined = Evaluation.generateXMLTree("E:\\Allen\\Linguistics\\mTurkFutureNotFutureCombinedOrderedSubtrees.xml")

//  combined.saveSVMLightMatrix("E:\\Allen\\Linguistics\\combinedSVMMatrix.txt", 214, svmForm = false)

//  Evaluation.printDIFF("E:\\Allen\\Linguistics\\diffFutureNotFuture.csv", Evaluation.compareDIFF(future, notFuture))

  future.saveLDASentenceMap("E:\\Allen\\LDA\\futureSelectedPattern", Some("E:\\Allen\\Linguistics\\diffFutureNotFuture(rel_value).csv"), Some(0))

  def generateLDABySentence(fileLoc: String, outputDir: String): Unit = {
    val output = Paths.get(outputDir)
    val input = CSVReader.open(new File(fileLoc))

    val lines = input.all()
    for (line <- lines) {
      val words = line(1).split(" ")
      val path = Paths.get(fileLoc+"\\"+line(0))
      Files.write(path, words.mkString("\t").getBytes, APPEND, CREATE)
    }
  }

  //for two XMLTransform()ed files to combine
  def combineVarroTreeFiles(file1: String, file2: String, output: String): Unit = {
    val sentenceNum = varroTransform(file1, output, 1, hasContinuedPart = true, isCountinuedPart = false)
    varroTransform(file2, output, sentenceNum, hasContinuedPart = false, isCountinuedPart = true)
  }

  //This is for SVMLight TreeBank
  def svmLightTransform(): Unit = {
    val reader = CSVReader.open(new File("E:\\Allen\\TwitterProject\\Pilot2_parse_flags_out_from_Sarah_w_past.csv"))

    val output = Paths.get("E:\\Allen\\Linguistics\\mTurksvmLightTree.txt")

    val it = reader.iterator
    it.next() //get rid of first line
    while(it.hasNext) {
      val row = it.next()
      if (!row(4).isEmpty) {
        val msg = row(49) + " |BT| " + row(4) + " |ET| " + "\r\n"
        //row 49: label 1/0,
        Files.write(output, msg.getBytes, APPEND, CREATE)
      }
    }
  }


  def xmlTransform(): Unit = {
    val lp = LexicalizedParser.loadModel("edu/stanford/nlp/models/lexparser/englishFactored.ser.gz", "-MAX_ITEMS","500000")

    val input:Input = Resource.fromFile("E:\\Allen\\Linguistics\\mTurkNotFutureRawSentence.txt")
    val file = new File("E:\\Allen\\Linguistics\\mTurkXMLTreebankNotFuture.txt")
    val printWriter = new PrintWriter(file)

    val lines = input.lines()

    for (line <- lines) {
      println("parsing starts: "+ line)
      val tree = lp.parse(line.toLowerCase)
      tree.indentedXMLPrint(printWriter, false) // no probability score needed
    }

    printWriter.close()

    println("done!")
  }

  /**
   * Must be applied after XMLTransform()
   * If OutputFile is the same, it will append!
   */
  def varroTransform(inputLoc: String, ouputLoc: String, startId: Int, hasContinuedPart: Boolean, isCountinuedPart: Boolean): Int = {
    val input:Input = Resource.fromFile(inputLoc)
    val output = Paths.get(ouputLoc)

    val lines = input.lines()

    var senId = startId //for every sentence
    var nodeId = startId //unique for every node in one sentence

    if (!isCountinuedPart)
      Files.write(output, "<treebank sourceFile=''>".getBytes, APPEND, CREATE)

    for (line <- lines) {
      if (line.contains("<ROOT>")) {
        //new sentence
        val msg= "<sentence id=\"" + senId + "\"> \r\n"
        Files.write(output, msg.getBytes, APPEND, CREATE)
      }

      val whiteSpace = line.split("<")(0)

      //for tags like PRP$, "$" will raise XML error
      var newLine = ""
      if (line.contains("$")) {
        newLine = line.replace("$", "dollar")
      }
      else{
        newLine = line
      }

      //main matching cases here:
      if (line.contains("</")) {
        //</S> </NP>
        val msg= whiteSpace + "</node> \r\n"
        Files.write(output, msg.getBytes, APPEND)
      }
      else if (line.contains("/>")) {
        val msg = whiteSpace + "<node edge=\"--\" id=\""  + nodeId + "\" label=\""  + newLine.trim.substring(1, newLine.trim.length-2) + "\" /> \r\n"
        nodeId += 1
        Files.write(output, msg.getBytes, APPEND)
      }
      else {
        //for a regular node
        val msg = whiteSpace + "<node edge=\"--\" id=\""  + nodeId + "\" label=\""  + newLine.trim.substring(1, newLine.trim.length-1) + "\" > \r\n"
        nodeId += 1
        Files.write(output, msg.getBytes, APPEND)
      }

      if (line.contains("</ROOT>")) {
        val msg= "</sentence> \r\n"
        senId += 1
        println(senId)
        Files.write(output, msg.getBytes, APPEND)
      }

    }

    if (!hasContinuedPart)
      Files.write(output, "</treebank>".getBytes, APPEND)

    println("Done!")
    println("Processed number of strings: " + senId)

    senId
  }

}