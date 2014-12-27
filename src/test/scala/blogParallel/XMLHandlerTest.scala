package blogParallel

import org.scalatest.FlatSpec
import blogParallel.XMLHandler._
import java.io.File

class XMLHandlerTest extends FlatSpec{

  "A NIO Traversal Function" should "get files" in {
    val files = nioTraverseDir("E:\\Jason\\blogs")
    assertResult(3385, "Should be 3385 files") {files.length}
  }

  "A XML info generation function" should "extract information in right order" in {
    val file = new File("E:\\Jason\\blogs\\11253.male.26.Technology.Aquarius.xml")
    val result = generateInfo(file)

    assertResult("14,July,2004", "Date should be: '20,July,2004' ") {
      result(1)._1
    }

    assertResult("My Dad has always wanted to go to  urlLink America . I have been several times, for holidays, to see friends or for work. I'd love to go with Dad and go on a long road trip. Would make a better blog than this.") {
      result(1)._2
    }
  }

  "XMLExtract function" should "generate a right Hashmap" in {
    val result = extractXML("E:\\Jason\\blogs_test")
    assume(result.get("11253.male.26.Technology.Aquarius.xml").isDefined)

    assertResult("14,July,2004", "Date should be: '20,July,2004' ") {
      result.get("11253.male.26.Technology.Aquarius.xml").get(1)._1
    }
  }

}
