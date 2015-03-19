package NYTAkka

import org.scalatest.FlatSpec

/**
 * Created by anie on 3/14/2015.
 */
class FileIteratorTest extends FlatSpec {

  "A file iterator" should "iterate over values" in {

    val file = FileIterator("E:\\Allen\\NYTFuture\\NYT", true)
    while (file.hasNext) {
//      println(file.nextLine())
      file.next()
    }

  }

  "A print header" should "print" in {
//    Start.printHeader(Some(Array(0,3,4,1,2)))
  }
}
