package NYTAkka

import java.nio.file.{Path, Paths, Files}
import java.nio.file.StandardOpenOption._
import blogParallel.Pattern._

import scala.collection.mutable.{ArrayBuffer, Seq}
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

/**
 * Created by anie on 3/17/2015.
 */
object FileOp {
  import Config._

  //border: mark where number vectors begin
  //printRow is a partially applied function
  //collapse is also partially applied (border)
  def compress(fileIterator: FileIterator, keyCol: Int, pageCol: Option[Int], printRow: ((String, Array[Int])) => Unit,
               convert: (Array[String]) => (ArrayBuffer[String], ArrayBuffer[Int])): Unit = {

    val acc = ArrayBuffer[ArrayBuffer[Int]]()
    var key = "1"
    var page = ""
    var currentLine = 0

    while (fileIterator.hasNext) {
      currentLine += 1
      if (currentLine % 100 == 0) printToTimer(currentLine)
      val rawline = fileIterator.next().split("\t")
      val line = convert(rawline)

      if (line._1(keyCol) != key && acc.nonEmpty) {
        printRow(key+"\t"+page, collapse(acc))
        acc.clear()
        key = line._1(keyCol)
        page = line._1(pageCol.get)
        acc += line._2 //add the current line to empty acc
      }
      else {
        key = line._1(keyCol)
        page = line._1(pageCol.get)
        acc += line._2
      }
    }

    //when Iterator ended, we print out remaining stuff
    if (acc.nonEmpty)
      printRow(key+"\t"+page, collapse(acc))

  }

  def collapse(all: ArrayBuffer[ArrayBuffer[Int]]): Array[Int] = {
    all.reduce{(first, second) => {
      first.zipWithIndex.map {e =>
        e._1 + second(e._2)
      }
    }
    }.toArray
  }

  //border is inclusive
  def convert(res: Array[String], border: Int): (ArrayBuffer[String], ArrayBuffer[Int]) = {
    val keys = ArrayBuffer.empty[String]
    val vector = ArrayBuffer.empty[Int]
    (0 to res.length - 1).foreach(e => {
      if (e <= border) keys+= res(e)
      else {
        if (isAllDigits(res(e))) vector += res(e).toInt
        else vector += 0
      }})
    (keys, vector)
  }

  def isAllDigits(x: String): Boolean = x forall Character.isDigit

  val timerPath = Paths.get(timer)
  val startTime = System.currentTimeMillis
  var now = System.currentTimeMillis()
  val totalLine = 42833581
  def printToTimer(currentLine: Double): Unit =  {
    now = System.currentTimeMillis()
    Files.write(timerPath, (s"""progress: $currentLine / $totalLine : ${currentLine / totalLine}""" + "\r\n" +
      s"current time: ${(now - startTime)/1000}s, " +
      s"expected: ${(((now - startTime) / currentLine) * (totalLine - currentLine))/1000}s").getBytes, CREATE, TRUNCATE_EXISTING)
  }


  def printParaHeader(): Unit = {
    val outByPara = Paths.get(outputFiles("byParagraph"))
    Files.write(outByPara, ((Array("ParaID", "PageID") ++ patternFuture ++ patternsPast ++ patternPresent).mkString("\t")+"\r\n").getBytes, CREATE, APPEND)
  }

  def printPageHeader(): Unit = {
    val outByPage = Paths.get(outputFiles("byPage"))
    Files.write(outByPage, ((Array("PageID") ++ patternFuture ++ patternsPast ++ patternPresent).mkString("\t")+"\r\n").getBytes, CREATE, APPEND)
  }

  def printToFile(out: Path, res: (String, Array[Int])): Unit = {
    Files.write(out, (res._1+"\t"+res._2.mkString("\t")+"\r\n").getBytes, CREATE, APPEND)
  }

}
