package blogParallel

import java.nio.file._
import java.io.{FileInputStream, File}
import scala.collection.immutable.HashMap
import scala.xml._

object XMLHandler {
  //the goal is to extract XML content
  //pair up three info:

  /**
   * Extract FileName/blogger_info: (Date, EntryContent)
   * Has to be mutable concerning the speed/efficiency
   * @param loc directory location
   */
  def extractXML(loc: String): HashMap[String, List[(String, String)]] = {
    val fileList = nioTraverseDir(loc)

    fileList.foldLeft(HashMap[String, List[(String, String)]]()) { (map, file) =>
      map.updated(file.getName, generateInfo(file))
    }
  }

  def generateInfo(doc: File):  List[(String, String)] = {
    val file = loadXML(doc)
    val dateList = file \ "date"
    val entryList = file \ "post"

    (0 to dateList.length - 1).map{n => (dateList(n).text.trim, entryList(n).text.trim)}.toList
  }

  def nioTraverseDir(loc: String): List[File] = {
    import scala.collection.JavaConversions._

    val directoryStream : DirectoryStream[Path] = Files.newDirectoryStream(Paths.get(loc))
    directoryStream.map(path => path.toFile).toList
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