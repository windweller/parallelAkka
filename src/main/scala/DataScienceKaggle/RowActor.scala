package dataScienceKaggle

import java.io.BufferedWriter

import akka.actor.{ActorRef, ActorLogging, Actor}
import com.github.tototoshi.csv.CSVWriter

import scala.annotation.tailrec
import scala.collection.mutable
import scala.collection.mutable.{ArrayBuffer, ListBuffer}


class RowActor(timer: ActorRef, printer: ActorRef,
               stableDictionaries: mutable.ArrayBuffer[Array[String]]) extends Actor with ActorLogging {
  import RowMsg._
  import TimerMsg._
  import Common._
  import PrinterMsg._

  val vector: ListBuffer[String] = ListBuffer()

  def receive = {
    case Row(lines) =>

      lines.foreach { line =>
        val segs = line.split(",")

        println(segs(0))

        segs(0) +: (1 to segs.length -1).map { i =>
          stableDictionaries(i-1).indexOf(segs(i)).toString
        }

        timer ! OneDone

        printer ! Print(vector.toIndexedSeq)
      }

    case CountCate(line) =>
      val segs = line.split(",")

        (1 to segs.length - 1).map{ i =>
           dictionaries(i-1)._2.add(segs(i))
        }


      timer ! OneDone
  }

//  @tailrec
//  final def returnSetElemIndex(set: mutable.Array[String], find: String, index: Int): Int = {
//    if (set.head != find) returnSetElemIndex(set.tail, find, index+1) else index
//  }

}

object RowMsg {
  case class Row(raw: ArrayBuffer[String])
  case class CountCate(raw: String)
}