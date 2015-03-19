package NYTAkka

import java.nio.file.Paths

import NYTAkka.Config._
import org.scalatest.FlatSpec

import scala.collection.mutable.{ArrayBuffer, Seq}

/**
 * Created by anie on 3/19/2015.
 */
class CompressTest extends FlatSpec {

  "compress" should "be able to compress paragraphs" in {
    FileOp.printParaHeader()

    FileOp.compress(FileIterator("E:\\Allen\\NYTFuture\\NYT_results\\nyt_by_sen.txt", header = true),
                1, Some(2), FileOp.printToFile(Paths.get(outputFiles("byParagraph")), _: (String, Array[Int])),
                FileOp.convert(_:Array[String], 4))
  }

}
