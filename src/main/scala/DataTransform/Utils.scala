package DataTransform

import java.io.{FileReader, BufferedReader, PrintWriter, File}
import java.nio.file.StandardOpenOption._
import java.nio.file.{Files, Paths}

import FolderReadingNIO.CSVHandler
import com.github.tototoshi.csv.CSVReader
import edu.stanford.nlp.parser.lexparser.LexicalizedParser
import edu.stanford.nlp.trees.Tree

import FileOp._

import scalax.io.{Resource, Input}

/**
 * Created by anie on 2/4/2015.
 */
object Utils {

  def dropEle(nth: Int, in: Array[String]): Array[String] = {
    in.view.zipWithIndex.filter{ _._2 != nth }.map{ _._1 }.toArray
  }

  def dropEle[T](nth: Set[Int], in: Array[T]): List[T] = {
    in.view.zipWithIndex.filter{case (t, index) => !nth.contains(index)}.map{ _._1 }.toList
  }


}
