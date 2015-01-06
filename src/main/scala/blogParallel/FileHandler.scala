package blogParallel

import java.nio.file._
import java.io.{FileInputStream, File}
import scala.collection.immutable.HashMap
import scala.xml._

case class FileHandler(loc: String) {
  //the goal is to extract XML content
  //pair up three info:

  var totalNumOfEntry = 0

  /**
   * Extract FileName/blogger_info: (Date, EntryContent)
   * Has to be mutable concerning the speed/efficiency
   */
  def extractXML(): HashMap[String, List[(String, String)]] = {

    val fileList = nioTraverseDir[File]((path) => path.toFile)

    fileList.foldLeft(HashMap[String, List[(String, String)]]()) { (map, file) =>
      map.updated(file.getName, generateInfo(file))
    }
  }

  def generateInfo(doc: File):  List[(String, String)] = {
    val file = loadXML(doc)
    val dateList = file \ "date"
    val entryList = file \ "post"

    (0 to dateList.length - 1).map{n => totalNumOfEntry += 1; (dateList(n).text.trim, entryList(n).text.trim)}.toList
  }

  def nioTraverseDir[A](transform: (Path) => A): List[A] = {
    import scala.collection.JavaConversions._

    val directoryStream : DirectoryStream[Path] = Files.newDirectoryStream(Paths.get(loc))
    directoryStream.map(path => transform(path)).toList
  }

  def loadXML(file: File) = {
    val parserFactory = new org.ccil.cowan.tagsoup.jaxp.SAXFactoryImpl
    val parser = parserFactory.newSAXParser()
    val source = new org.xml.sax.InputSource(new FileInputStream(file))
    val adapter = new scala.xml.parsing.NoBindingFactoryAdapter
    val feed = adapter.loadXML(source, parser)
    feed
  }

}