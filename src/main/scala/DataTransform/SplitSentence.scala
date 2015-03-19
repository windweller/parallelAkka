package DataTransform

import java.io.{StringReader, FileReader, BufferedReader}

import FolderReadingNIO.CSVHandler
import com.github.tototoshi.csv.CSVWriter
import edu.stanford.nlp.process.DocumentPreprocessor

/**
 * This class is used to split sentences
 * it assumes a csv file with rows; every row has a paragraph (or a group of sentences)
 * it spits out a csv file that has every row for every sentence
 * @param loc csv file in
 * @param out csv file out
 */
case class SplitSentence(loc: String, out:String, sen: Int) {

  import scala.collection.JavaConversions._

  val br = new BufferedReader(new FileReader(loc))
  val writer = CSVWriter.open(out, append=true)

  def process(): Unit = {
    var line =  br.readLine()

    var count = 0

    while (line != null) {

      val lines = CSVHandler.parseLine(line)
      println(lines(0))

      //parse the sentence line
      val processorIterator = new DocumentPreprocessor(new StringReader(lines(sen)))
      val it = processorIterator.iterator()

      while (it.hasNext) {
        //print the stuff out
        writer.writeRow(List(lines(0) + "_" + count, lines(0), it.next().toArray.mkString(" ")))
        count += 1
      }

      count = 0 //clear the counter
      line = br.readLine()
    }
  }
}
