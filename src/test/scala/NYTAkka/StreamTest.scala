package NYTAkka

import java.io.{FileOutputStream, File}
import java.nio.ByteBuffer

/**
 * Created by anie on 3/14/2015.
 */
object StreamTest extends App {


  val file = FileIterator("E:\\Allen\\NYTFuture\\NYT", header = true)
  Start("E:\\Allen\\NYTFuture\\NYT").stream(file)


}
