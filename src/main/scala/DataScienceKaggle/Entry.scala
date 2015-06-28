package dataScienceKaggle

import java.io._
import java.nio.file.{Paths, Files}
import java.nio.ByteBuffer
import java.text.NumberFormat
import java.util.Scanner

import akka.actor.{ActorRef, Props, ActorSystem}
import akka.stream._
import akka.stream.scaladsl.{Sink, Source}
import dataScienceKaggle.TimerMsg.OneDone
import scala.collection.mutable.{ArrayBuffer, ListBuffer}
import scala.util.Try
import com.typesafe.config.{ConfigFactory, Config}
import scala.collection.mutable

object Entry {

  def main(args: Array[String]): Unit = {
    import RowMsg._
    import PrinterMsg._
    import TimerMsg._

    val conf: Config = ConfigFactory.load()
    implicit val system: ActorSystem = ActorSystem("Kaggle", conf) //added actor logging info

    val writer = new BufferedWriter(new FileWriter("E:\\Allen\\DataScience\\vectorizedSample_3_Train300000.csv", false))

    val printer = system.actorOf(Props(classOf[Printer], writer), name = "Printer")

    val timer = system.actorOf(Props(classOf[Timer], "E:\\Allen\\timer_kaggle.txt", printer), name = "Timer")

    val stableDictionaries = readInFactorsDic()

//    val rowActors = (1 to 10).map { i =>
//       system.actorOf(Props(classOf[RowActor], timer, printer, stableDictionaries), name="RowActor"+i)
//    }

//    val content = readLargeFile("E:\\Allen\\DataScience\\train\\train.csv", timer)

//    (0 to 9).map { i =>
//      println("assigned portion: " + i)
//      rowActors(i) ! Row(content(i))
//    }

    val inputStream = new FileInputStream("E:\\Allen\\DataScience\\SampleFromTrain300000.csv")
    val sc = new Scanner(inputStream, "UTF-8")

    //    val csvFile = Files.newByteChannel(Paths.get("E:\\Allen\\DataScience\\train\\train.csv"))
    //    val buf = ByteBuffer.allocateDirect(1000)

    //    var line =  csvFile.readLine()
    //
    //    line = csvFile.readLine() //skip first line

    implicit val materializer =  ActorFlowMaterializer()

    sc.nextLine()
    val fileSource = Source(() => Iterator.continually(sc.nextLine()))

    import system.dispatcher

    fileSource.buffer(15000, OverflowStrategy.backpressure).map {line =>
      line.split(",")
    }
      .map { segs =>
      segs(0) +: (1 to segs.length -1).map { i =>
        stableDictionaries(i-1)(segs(i))
      }
    }
      .runForeach{(segs) =>
      println(segs(0))
      timer ! OneDone //not sure if this works
      printer ! Print(segs)
    }.onComplete { _ =>
      Try {
        sc.close()
        inputStream.close()
      }
      system.shutdown()
    }


    //now we have all the lines

//    def processLine(line: String): Unit = {
//      val vector: ListBuffer[String] = ListBuffer()
//      val segs = line.split(",")
//
//      println(segs(0))
//
//      (1 to segs.length - 1).map {i =>
//        val factorArray = stableDictionaries(i-1)
//        vector += factorArray._2.indexOf(segs(i)).toString   //get the factor level of string
//      }
//
//      timer ! OneDone
//
//      printer ! Print(vector.toList)
//    }

  }

  //40428946 rows string
  def readLargeFile(loc: String, timer:ActorRef): Array[ArrayBuffer[String]] = {
    val fileReader = new BufferedReader(new FileReader(loc))
    fileReader.readLine()
    var line = fileReader.readLine()

    val totalContent = Array.fill(10)(ArrayBuffer[String]())
    var lineCounter = 0

    while (line != null) {

      totalContent(lineCounter % 10) :+ line

      lineCounter += 1
      timer ! OneDone
      line = fileReader.readLine()
    }

    totalContent
  }

  def readInFactorsDic(): mutable.ArrayBuffer[mutable.HashMap[String, String]] = {
    val csvReader = new BufferedReader(new FileReader("E:\\Allen\\DataScience\\factors.csv"))
    var line = csvReader.readLine()
    val dictionaries = mutable.ArrayBuffer[mutable.HashMap[String, String]]()

    while (line != null) {
      val segs = line.split(",").tail

      (0 to segs.length).foreach(t => dictionaries :+ segs(t) -> t)

      line = csvReader.readLine()
    }

    line = null

    csvReader.close()

    dictionaries
  }

  def readInFactors(): mutable.ArrayBuffer[(String, Array[String])] = {
    val csvReader = new BufferedReader(new FileReader("E:\\Allen\\DataScience\\factors.csv"))
    var line = csvReader.readLine()
    val dictionaries = mutable.ArrayBuffer[(String, Array[String])]()

    while (line != null) {
      val segs = line.split(",")
      dictionaries += (segs(0) -> segs.tail)

      line = csvReader.readLine()
    }

    line = null

    csvReader.close()

    dictionaries
  }
}

object Common {
  val dictionaries = mutable.ArrayBuffer[(String, mutable.Set[String])]()
}

// def timer(): Unit = {
//    currentRows += 1
//    currentTime = System.currentTimeMillis()
//
//    if (currentRows % 1000 == 0) {
//      val timerWriter = new BufferedWriter(new FileWriter("E:\\Allen\\timer_kaggle.txt", false))
//
//      timerWriter.write("PatternProgress: "+currentRows + " / " + totalRows + " => " + percentFormat.format(currentRows/totalRows))
//      timerWriter.newLine()
//      val expectedSecs = ((currentTime - startTime) / currentRows) * (totalRows - currentRows)
//      timerWriter.newLine()
//
//      timerWriter.write("Spent Time: "+ ((currentTime - startTime)/1000) + "s expected time: " + (expectedSecs/1000) + "s")
//      timerWriter.close()
//    }
//  }
