package DataTransform

import java.io.{FileReader, BufferedReader, PrintWriter, File}
import java.nio.file.StandardOpenOption._
import java.nio.file.{Files, Paths}

import DataTransform.FileOp.{CSV, Text, Doc}
import FolderReadingNIO.CSVHandler
import com.github.tototoshi.csv.CSVReader
import edu.stanford.nlp.parser.lexparser.LexicalizedParser
import edu.stanford.nlp.trees.Tree

import scalax.io.{Resource, Input}

/**
 * Created by anie on 2/13/2015.
 */
object Transform {

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


  def svmLightTransform(loc:String, outLoc: String, classificationCol: Int, parsedTreeCol: Int): Unit = {

  }

  //This is for SVMLight TreeBank
  def svmLightTKTransform(loc:String, outLoc: String, classificationCol: Int, parsedTreeCol: Int): Unit = {
    //should be CSV file
    val reader = CSVReader.open(new File(loc))

    val output = Paths.get(outLoc)

    val it = reader.iterator
    it.next() //get rid of first line
    while(it.hasNext) {
      val row = it.next()
      if (!row(parsedTreeCol).isEmpty) {
        val msg = row(classificationCol) + " |BT| " + row(parsedTreeCol) + " |ET| " + "\r\n"
        //row 49: label 1/0,
        Files.write(output, msg.getBytes, APPEND, CREATE)
      }
    }
  }


  /**
   * This is the first step to transform
   * Text file parses sentences
   * CSV file assumes already built trees
   * @param in
   * @param out "E:\\Allen\\Linguistics\\mTurkXMLTreebankNotFuture.txt" could be .xml not .txt
   */
  def xmlTransform(in: Doc, row: Int, out: String): Unit = {

    import scala.collection.JavaConversions._

    val printWriter = new PrintWriter(new File(out))

    in match {
      case Text(f) =>
        val lp = LexicalizedParser.loadModel("edu/stanford/nlp/models/lexparser/englishFactored.ser.gz", "-MAX_ITEMS","500000")

        val input:Input = Resource.fromFile(f)

        val lines = input.lines()

        for (line <- lines) {
          println("parsing starts: "+ line)
          val tree = lp.parse(line.toLowerCase)
          tree.indentedXMLPrint(printWriter, false) // no probability score needed
        }

      case CSV(f, header, _) =>
        val br = new BufferedReader(new FileReader(f))
        var line =  br.readLine()

        if (header) line = br.readLine() //skip header

        while (line != null) {
          val lines = CSVHandler.parseLine(line)
          println(lines(row-1))
          val tree = Tree.valueOf(lines(row)) //build the tree from csv
          tree.indentedXMLPrint(printWriter, false)

          line = br.readLine()
        }
    }

    printWriter.close()
    println("done!")

  }

  /**
   * Must be applied after XMLTransform()
   * If OutputFile is the same, it will append!
   */
  def varroTransform(inputLoc: String, ouputLoc: String, startId: Int, hasContinuedPart: Boolean, isCountinuedPart: Boolean): Int = {
    //    val input:Input = Resource.fromFile(inputLoc)

    val br = new BufferedReader(new FileReader(inputLoc))
    var line =  br.readLine()

    val output = Paths.get(ouputLoc)

    var senId = startId //for every sentence
    var nodeId = startId //unique for every node in one sentence

    if (!isCountinuedPart)
      Files.write(output, "<treebank sourceFile=''>".getBytes, APPEND, CREATE)

    while (line != null) {
      if (line.contains("<ROOT>")) {
        //new sentence
        val msg= "<sentence id=\"" + senId + "\"> \r\n"
        Files.write(output, msg.getBytes, APPEND, CREATE)
      }

      val whiteSpace = line.split("<")(0)

      //for tags like PRP$, "$" will raise XML error

      val newLine = line.replace("$", "dollar").trim

      //main matching cases here:
      if (line.contains("<//>")) { //we are matching a special case <//>, otherwise there'd be error
        //not gonna write anything
      }
      else if (line.contains("</")) {
        //</S> </NP>
        val msg= whiteSpace + "</node> \r\n"
        Files.write(output, msg.getBytes, APPEND)
      }
      else if (line.contains("/>")) {
        val msg = whiteSpace + "<node edge=\"--\" id=\""  + nodeId + "\" label=\""  + newLine.substring(1, newLine.length-2) + "\" /> \r\n"
        nodeId += 1
        Files.write(output, msg.getBytes, APPEND)
      }
      else {
        //for a regular node
        val msg = whiteSpace + "<node edge=\"--\" id=\""  + nodeId + "\" label=\""  + newLine.substring(1, newLine.length-1) + "\" > \r\n"
        nodeId += 1
        Files.write(output, msg.getBytes, APPEND)
      }

      if (line.contains("</ROOT>")) {
        val msg= "</sentence> \r\n"
        senId += 1
        println(senId)
        Files.write(output, msg.getBytes, APPEND)
      }

      line = br.readLine()
    }

    if (!hasContinuedPart)
      Files.write(output, "</treebank>".getBytes, APPEND)

    println("Done!")
    println("Processed number of strings: " + senId)

    senId
  }

}
