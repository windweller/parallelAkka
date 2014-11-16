package DataTransform

import java.nio.file.StandardOpenOption._
import scala.xml._
import com.github.tototoshi.csv.{CSVWriter, CSVReader}
import java.io.File
import java.nio.file.{Files, Paths}

object Evaluation {

  def generateXMLTree(doc: String): XMLtree = {

    val file = XML.loadFile(doc)
    val treeList = file \ "subtree" \ "tree" //a list of future subtrees
    val treeAddressList = file \ "subtree" \ "addresses"
    val rootCountList = (file \ "subtree").map(node => node.attribute("rootCount"))
    XMLtree(file, treeList, treeAddressList, rootCountList)

  }

  //formula is: F / #Fsentence - NotFuture / #NFsentence
  //always left - right
  def compareDIFF(xmlA: XMLtree, xmlB: XMLtree): Map[String, String] = {
//    val xmlARulesCount = for (rule <- xmlA.xmlTreesToParen(); count <- xmlA.rootCountList) yield (rule, count.get)
    val xmlARules = xmlA.xmlTreesToParen()
    val xmlBRules = xmlB.xmlTreesToParen()

    val xmlARulesCount = (0 to xmlARules.length - 1).map { i => (xmlARules(i), xmlA.rootCountList(i).get)}.toMap
    val xmlBRulesCount = (0 to xmlBRules.length - 1).map { i => (xmlBRules(i), xmlB.rootCountList(i).get)}.toMap

    if (xmlARules.length >= xmlBRules.length) generateDiff(xmlARulesCount, xmlARules.length, xmlBRulesCount, xmlBRules.length, leftRightPos = true)
    else generateDiff(xmlBRulesCount, xmlBRules.length, xmlARulesCount, xmlARules.length, leftRightPos = false)
  }

  //leftRightPositive tells whether the left one is the one on the left of subtraction
  // true: left - right; false: right - left
  private def generateDiff(xmlLarge:Map[String, Seq[Node]], xmlLargeLength: Int,
                           xmlSmall:Map[String, Seq[Node]], xmlSmallLength: Int, leftRightPos: Boolean): Map[String, String] = {
    xmlLarge.map{ m =>
      if (xmlSmall.get(m._1).isEmpty)
        leftRightPos match {
          case true =>  m._1 -> (m._2.text.toFloat / xmlLargeLength).toString
          case false => m._1 -> ( 0.0 - m._2.text.toFloat / xmlLargeLength ).toString
        }
      else
        leftRightPos match {
          case true => m._1 -> (m._2.text.toFloat / xmlLargeLength - xmlSmall.get(m._1).get.text.toFloat / xmlSmallLength).toString
          case false => m._1 -> (xmlSmall.get(m._1).get.text.toFloat / xmlSmallLength - m._2.text.toFloat / xmlLargeLength).toString
        }
    }.toMap
  }

  def printDIFF(fileLoc: String, result: Map[String, String]): Unit = {
    val file = new File(fileLoc)
    if(!file.exists()) file.createNewFile()

    val writer = CSVWriter.open(file)

    result.map( r => writer.writeRow(Seq(r._1, r._2)))

    writer.close()
  }

  case class XMLtree(xmlFile: Elem, treeList:NodeSeq, addressList:NodeSeq, rootCountList: Seq[Option[Seq[Node]]]) {

    /**
     * Format is CSV, sentence as first column, then pattern
     * @param fileLoc csv file loc
     */
    def saveSentenceWithSubtree(fileLoc: String): Unit = {
      val result = generateLDASentenceMap()
      val path = Paths.get(fileLoc)

      result.map{r =>
        Files.write(path, (r._1 + "," + r._2.mkString(",") + "\n").getBytes, APPEND, CREATE)
      }
    }

    /**
     * For LDA, generate files, each file = one sentence
     * each file contains the sentence's pattern trees, in parentheses
     * @return
     */
    def generateLDASentenceMap(): Map[String, List[String]] = {
      //a matrix, the row number will be the number of sentences in total, assuming unknown
      //the column number will be the pattern true, obtained by: treeList.length (same as subtreeList.length)

      val parenRules = this.xmlTreesToParen()

      var i = -1
      (Map[String, List[String]]() /: addressList) {
        {(map, sentenceList) =>
            //each sentenceList is one address collection
          val sentences = sentenceList \ "node" \\ "@id"
          i += 1
          (map /: sentences) {
            (oMap, sentence) =>
              val sentenceArray = sentence.text.split(":")
              val sentenceId = sentenceArray(0) + "_" + sentenceArray(1)
              oMap + (sentenceId -> oMap.getOrElse(sentenceId, List[String]()).+:(parenRules(i)))
          }
        }
      }
    }

    /**
     * Construct LDA with given constraints
     * @param constraintFile must be one of those diff files (abs or rel)
     * @param critera compare value to this criter, true pass, false fail
     */
    def generateLDASentenceMapWithConstraints(constraintFile: String, critera: Int): Map[String, List[String]] = {

      val reader = CSVReader.open(new File(constraintFile))
      val lines = reader.all()

      val patternSelection = (Map[String, Float]() /: lines) {
        (map, line) =>
          map.updated(line(1), line(0).toFloat)
      }

      val parenRules = this.xmlTreesToParen()

      var i = -1
      (Map[String, List[String]]() /: addressList) {
        {(map, sentenceList) =>
          //each sentenceList is one address collection
          val sentences = sentenceList \ "node" \\ "@id"
          i += 1
          (map /: sentences) {
            (oMap, sentence) =>
              val sentenceArray = sentence.text.split(":")
              val sentenceId = sentenceArray(0) + "_" + sentenceArray(1)
              if (patternSelection.get(parenRules(i)).nonEmpty) {
                if (patternSelection.get(parenRules(i)).get > 0) {
                  oMap + (sentenceId -> oMap.getOrElse(sentenceId, List[String]()).+:(parenRules(i)))
                }
                else oMap
              }
              else oMap
          }
        }
      }

    }

    /**
     * Must create files inside this directory
     * @param dirLoc base directory for all files
     */
    def saveLDASentenceMap(dirLoc: String, constraintsFile: Option[String], constraintsVal: Option[Int]): Unit = {
      val result = if (constraintsFile.nonEmpty && constraintsVal.nonEmpty)
        generateLDASentenceMapWithConstraints(constraintsFile.get, constraintsVal.get)
      else generateLDASentenceMap()

      result.map{r =>
        val path = Paths.get(dirLoc+"\\"+r._1)
        Files.write(path, r._2.mkString("\t").getBytes, APPEND, CREATE)
      }
    }

    /**
     * generate a map id = 1:1 or -1:1 (first is label, second is position of the sentence)
     * @param groupBound MUST be the starting ID of the second group.
     * @return
     */
    def generateSVMLightMatrix(groupBound: Int, svmForm: Boolean): Map[String, List[Int]] = {

      var i = -1
      (Map[String, List[Int]]() /: addressList) {
        {(map, sentenceList) =>
          //each sentenceList is one address collection
          val sentences = sentenceList \ "node" \\ "@id"
          i += 1 //counter for the number of pattern trees
          (map /: sentences) {
            (oMap, sentence) =>
              val sentenceArray = sentence.text.split(":")
              if (svmForm) {
                val sentenceId = (if (sentenceArray(1).toInt < groupBound) 1 else -1 ) + ":" + sentenceArray(1)
                oMap + (sentenceId -> oMap.getOrElse(sentenceId, List[Int]()).+:(i))
              }
              else {
                val sentenceId = (if (sentenceArray(1).toInt < groupBound) 1 else 0 ) + ":" + sentenceArray(1)
                oMap + (sentenceId -> oMap.getOrElse(sentenceId, List.fill(treeList.length)(0)).updated(i, 1))
              }
          }
        }
      }
    }

    /**
     * Change the format
     * @param fileLoc file location
     * @param bound MUST be the starting ID of the second group.
     * @param svmForm indicate if instead of 1 or -1, the first row would be 1, 0 indicating Future or Not Future
     */
    def saveSVMLightMatrix(fileLoc: String, bound: Int, svmForm: Boolean): Unit = {
      val result = generateSVMLightMatrix(bound, svmForm)
      val output = Paths.get(fileLoc)

      if (svmForm) {
        result.map{sentence =>
          val head = sentence._1.split(":")(0)
          Files.write(output, (head + " " + sentence._2.sorted.mkString(":1 ") + ":1" + "\n").getBytes, APPEND, CREATE)
        }
      }
      else {
        result.map{sentence =>
          val head = sentence._1.split(":")(0)
          Files.write(output, (head + "," + sentence._2.sorted.mkString(",") + "\n").getBytes, APPEND, CREATE)
        }
      }


      println("done")
    }

    //generate basic stuff
    def generateSentenceMatrixMap(): Map[String, List[Int]] = {

      var i = -1
      (Map[String, List[Int]]() /: addressList) {
        {(map, sentenceList) =>
          //each sentenceList is one address collection
          val sentences = sentenceList \ "node" \\ "@id"
          i += 1
          (map /: sentences) {
            (oMap, sentence) =>
              val sentenceArray = sentence.text.split(":")
              val sentenceId = sentenceArray(0) + ":" + sentenceArray(1)
              oMap + (sentenceId -> oMap.getOrElse(sentenceId, List.fill(treeList.length)(0)).updated(i, 1))
          }
        }
      }
    }

    /**
     * This is for basic Clustering
     * @param fileLoc
     */
    def saveSentenceMatrixMap(fileLoc: String): Unit = {
      val result = generateSentenceMatrixMap()
      val file = new File(fileLoc)
      if(!file.exists()) file.createNewFile()

      val writer = CSVWriter.open(file)

      result.map(r => writer.writeRow(Seq(r._1) ++ r._2))
    }

    def xmlTreesToParen(): Seq[String] = {
      treeList.map(node => xmlTreeToParen(node))
    }

    def saveParenRule(fileLoc: String, parenRules: Seq[String]): Unit = {
      val file = new File(fileLoc)
      if(!file.exists()) file.createNewFile()

      val writer = CSVWriter.open(file)

      (0 to parenRules.length - 1).map { i =>
        //add address in the future!!! address is already a sequence
        writer.writeRow(Seq(i, parenRules(i), rootCountList(i).get.text))
      }

      writer.close()
    }

    /**
     * Transform XML tree to parentheses tree
     */
    private def xmlTreeToParen(tree: NodeSeq): String = {
      //base case
      if (tree.isEmpty) {
        ""
      }
      else{
        (0 to tree.length - 1).map {childNum =>
          //this is on the node's own level
          val label = tree(childNum).attribute("label").getOrElse("")

          if (label == "") {
            xmlTreeToParen(tree(childNum).child)
          } else {
            label + "(" + xmlTreeToParen(tree(childNum).child) + ")"
          }
        }.mkString("")
      }
    }

  }
}
