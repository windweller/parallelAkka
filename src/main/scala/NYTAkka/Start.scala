package NYTAkka

import java.io.File
import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.nio.file.{Path, Files, Paths}
import java.nio.file.StandardOpenOption._

import akka.actor.{ActorLogging, ActorSystem}
import akka.stream.ActorFlowMaterializer
import akka.stream.scaladsl._
import scala.concurrent.{ExecutionContext, Future}
import blogParallel.Pattern._
import com.typesafe.config.{ConfigFactory, Config}
import edu.stanford.nlp.trees.Tree
import org.slf4j.LoggerFactory

import scala.collection.immutable.HashMap
import scala.collection.mutable._
import scala.util.{Failure, Success}



object Config {
  val DataStructure = Array("id" -> 0, "rawSen" -> 1, "tree" -> 2, "paraID" -> 3, "pageID" -> 4)

  val outputFiles = HashMap("bySentence" -> "E:\\Allen\\NYTFuture\\NYT_results\\nyt_by_sen.txt",
    "byParagraph" -> "E:\\Allen\\NYTFuture\\NYT_results\\nyt_by_para.txt",
    "byPage" -> "E:\\Allen\\NYTFuture\\NYT_results\\nyt_by_page.txt")

  val timer = "E:\\Allen\\NYTFuture\\NYT_results\\timer.txt"
}

/**
 * This is used to kick start the process
 * It either uses Akka actor, future, or stream
 * right now it should default to Akka stream
 *
 * Akka Stream version uses stream API and Future (to write)
 * turn Start to a class to produce more modular code (easily)
 *
 * @param corpus "E:\\Allen\\NYTFuture\\NYT_sample"
 */
case class Start(corpus: String) {

  //this will be updated if we change the structure
  import Config._

  val DataStructureMap = DataStructure.toMap

  val log = LoggerFactory.getLogger("NYT") //let's try this


  val totalLine = countTotalLines(corpus)


  def stream(fileIterator: FileIterator): Unit = {
    //match every sentence, produce sentence-based
    val conf: Config = ConfigFactory.load()
    implicit val system: ActorSystem = ActorSystem("NYTWIKI", conf)

    import system.dispatcher

    implicit val materializer = ActorFlowMaterializer()

    if (fileIterator.header) printHeader(Some(Array(0,3,4,1,2)))

    val primeSource: Source[Array[String], Unit] = Source(() => fileIterator).map(s => s.split("\t"))

    //write to file sink, this part doesn't change much

    val bySentenceSink = Sink.foreach[(Array[String], Array[Int])](e => sentenceSink(e)) //sentenceSink(e)

    val byParagraphSink = Sink.foreach[(Array[String], Array[Int])](e => e)

    val byPageSink = Sink.foreach[(Array[String], Array[Int])](e => e)


    val materialized = FlowGraph.closed(bySentenceSink, byParagraphSink)((_, _)) { implicit builder =>
      (bySenSink, byParaSink) =>
      import FlowGraph.Implicits._

      val bcast = builder.add(Broadcast[(Array[String], Array[Int])](2))

      primeSource ~> Flow[Array[String]].map(r => patternMatching(r)) ~> bcast.in

      bcast.out(0) ~> bySenSink.inlet
      bcast.out(1) ~> byParaSink.inlet

    }.run()

    materialized._1.onComplete {
      case Success(_) => system.shutdown()
      case Failure(e) =>
        println(s"Failure: ${e.getMessage}")
        system.shutdown()
    }

  }

  import ExecutionContext.Implicits.global

  val timerPath = Paths.get(timer)
  val startTime = System.currentTimeMillis
  var now = System.currentTimeMillis()
  def printToTimer(currentLine: Double): Unit =  {
    now = System.currentTimeMillis()
    Files.write(timerPath, (s"""progress: $currentLine / $totalLine : ${currentLine / totalLine}""" + "\r\n" +
                            s"current time: ${(now - startTime)/1000}s, " +
                 s"expected: ${(((now - startTime) / currentLine) * (totalLine - currentLine))/1000}s").getBytes, CREATE, TRUNCATE_EXISTING)
  }

  def countTotalLines(loc: String): Int = {
    val file = FileIterator(loc, header = true)
    var i = 0
    while (file.hasNext) {
      file.next()
      i += 1
    }
    i
  }

  val outBySen = Paths.get(outputFiles("bySentence"))
  val outByPara = Paths.get(outputFiles("byParagraph"))
  val outByPage = Paths.get(outputFiles("byPage"))

  var currentLine = 0.0
  def sentenceSink(res: (Array[String], Array[Int])): Unit = {
    printToFile(outBySen, (res._1.mkString("\t"), res._2.mkString("\t")))
    currentLine += 1
    if (currentLine % 100 == 0)
      printToTimer(currentLine)
  }

  //it saves and cleans
//  val paragraphCol = 1
//  val pageCol = 2
//  val currentTotalPara = ListBuffer[(Array[String], Array[Int])]()
//  var currentParaID = ""

  //Array(SentenceID, Parag, Page, rawSentence, parsedSentence), Array(Matches)
//  def paragraphSink(res: (Array[String], Array[Int]), fileIterator: FileIterator): Unit = {
//
//    if (!fileIterator.hasNext) {
//      printToFile(outByPara, currentTotalPara.foldLeft[(String, Seq[Int])](("", Seq[Int]())){(acc, n) =>
//        (n._1(paragraphCol) + "\t" + n._1(pageCol) + "\t", n._2.zipWithIndex.map {nn =>
//          if (acc._2.isEmpty) nn._1
//          else nn._1 + acc._2(nn._2)
//        })
//      })
//      currentTotalPara.clear()
//    }
//    else if (currentParaID != res._1(paragraphCol) && currentTotalPara.nonEmpty) {
//      printToFile(outByPara, currentTotalPara.foldLeft[(String, Seq[Int])](("", Seq[Int]())){(acc, n) =>
//        (n._1(paragraphCol) + "\t" + n._1(pageCol) + "\t", n._2.zipWithIndex.map {nn =>
//          if (acc._2.isEmpty) nn._1
//          else nn._1 + acc._2(nn._2)
//        })
//      })
//      currentTotalPara.clear()
//      paragraphSink(res, fileIterator) //pass it back in
//    }
//    else { //when currentTotalPara is empty or ID don't match, we override ID
//      currentParaID = res._1(paragraphCol)
//      currentTotalPara += res
//    }
//  }

//  val currentTotalPage = ListBuffer[(Array[String], Array[Int])]()
//  var currentPageID = ""
//  def pageSink(res: (Array[String], Array[Int]), fileIterator: FileIterator): Unit = {
//
//    if (!fileIterator.hasNext) {
//      printToFile(outByPage, currentTotalPage.foldLeft[(String, Seq[Int])](("", Seq[Int]())){(acc, n) =>
//        (n._1(pageCol) + "\t", n._2.zipWithIndex.map {nn =>
//          if (acc._2.isEmpty) nn._1
//          else nn._1 + acc._2(nn._2)
//        })
//      })
//      currentTotalPage.clear()
//    }
//    else if (currentPageID != res._1(pageCol) && currentTotalPage.nonEmpty) {
//      printToFile(outByPage, currentTotalPage.foldLeft[(String, Seq[Int])](("", Seq[Int]())){(acc, n) =>
//        println(n._1.mkString(" "))
//        (n._1(pageCol) + "\t", n._2.zipWithIndex.map {nn =>
//          if (acc._2.isEmpty) nn._1
//          else nn._1 + acc._2(nn._2)
//        })
//      })
//      currentTotalPage.clear()
//      paragraphSink(res, fileIterator) //pass it back in
//    }
//    else { //when currentTotalPara is empty or ID don't match, we override ID
//      currentPageID = res._1(pageCol)
//      currentTotalPage += res
//    }
//  }


  //before printing, print the head
  def printToFile(out: Path, res: (String, String)): Unit = {
    Files.write(out, (res._1+ "\t" +res._2+"\r\n").getBytes, CREATE, APPEND)
  }


  //this signifies order or header: Array(SentenceID, Parag, Page, rawSentence, parsedSentence), Array(Matches)
  //correct way: printHeader(Some(Array(0,3,4,1,2)))
  //if null, original header order
  def printHeader(headers: Option[Array[Int]]): Unit = {

    val structureHeaders = DataStructure.map(n => n._1)
    val headerToPrint = if (headers.isEmpty) structureHeaders ++ patternFuture ++ patternsPast ++ patternPresent
                         else headers.get.map(n => structureHeaders(n)) ++ patternFuture ++ patternsPast ++ patternPresent

    Files.write(outBySen, (headerToPrint.mkString("\t")+ "\r\n").getBytes, CREATE, APPEND)
    Files.write(outByPara, ((Array("ParaID", "PageID") ++ patternFuture ++ patternsPast ++ patternPresent).mkString("\t")+"\r\n").getBytes, CREATE, APPEND)
    Files.write(outByPage, ((Array("PageID") ++ patternFuture ++ patternsPast ++ patternPresent).mkString("\t")+"\r\n").getBytes, CREATE, APPEND)

  }

  import edu.stanford.nlp.trees.tregex.TregexPattern
  import blogParallel.Pattern._
  val patterns = (for (pattern <- patternFuture ++ patternsPast ++ patternPresent) yield TregexPattern.compile(pattern)).par
  val patternsWithIndex = patterns.zipWithIndex

  /**
   *
   * @param row
   * @return Array(SentenceID, Parag, Page, rawSentence, parsedSentence), Array(Matches)
   */
  def patternMatching(row: Array[String]): (Array[String], Array[Int]) = {

    val stats = Array.fill[Int](patterns.size)(0)

    //this could be faster
    patternsWithIndex.foreach{ e =>
      try {
        val matcher = e._1.matcher(Tree.valueOf(row(DataStructureMap("tree"))))
        if (matcher.find()) {
          stats(e._2) = stats(e._2) + 1
        }
      } catch {
        case e: NullPointerException =>
          log.info("NULL Pointer with " + row(DataStructureMap("id")) + " : " + row(DataStructureMap("rawSen")))
      }
    }

    (Array(row(DataStructureMap("id")), row(DataStructureMap("paraID")),
              row(DataStructureMap("pageID")), row(DataStructureMap("rawSen")), row(DataStructureMap("tree"))), stats)
  }

}
