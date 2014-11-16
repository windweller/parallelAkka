package TwitterFuture

import java.util.concurrent.Executors

import akka.actor._
import com.github.tototoshi.csv.{CSVWriter, CSVReader}
import java.io.File
import edu.stanford.nlp.parser.lexparser.{Options, LexicalizedParser}
import edu.stanford.nlp.trees.Tree
import edu.stanford.nlp.trees.tregex.TregexPattern
import TimerMsg._
import scala.concurrent.ExecutionContext
import scala.collection.JavaConverters._

/**
 * Mostly use Future asynchronous
 * style instead of Actor
 */
object Entry extends App {

  implicit val ec = ExecutionContext.fromExecutor(Executors.newFixedThreadPool(20))

  val inputFileLoc = "E:\\Allen\\TwitterProject\\Pilot2_parse_flags_out_from_Sarah_w_past.csv" //load original sentences
  val timerLoc = "E:\\Allen\\timer.txt"
  val lp = LexicalizedParser.loadModel("edu/stanford/nlp/models/lexparser/englishFactored.ser.gz", "-MAX_ITEMS","500000")

  val system: ActorSystem = ActorSystem("Twitter")
  val timer = system.actorOf(Props(new Timer(timerLoc)), "Timer")

  //Hum3Fut is Sarah's; Hum1Fut is Jack's
  val jackFutureFile = "E:\\Allen\\TwitterProject\\Future\\Jack.txt"
  val sarahFutureFile = "E:\\Allen\\TwitterProject\\Future\\Sarah.txt"

  convertToTxt(inputFileLoc)

  def convertToTxt(inputFileLoc: String) {
    println("start reading!")

    val rows = CSVReader.open(new File(inputFileLoc)).all().drop(1)
    timer ! TotalTask(rows.size)

    val testTree = lp.parse(rows(1)(6).toLowerCase)

    val children = testTree.children()
    val subtrees = testTree.subTreeList().asScala
    println(children(0).children().size)
    children(0).children()(1).pennPrint()

//    println(subtrees.size)
//    subtrees(2).pennPrint()

    system.shutdown()

//    val sarah = for (row <- rows if row(47) == "1") yield row(6)
//    parse(sarah)

//    val jack = for (row <- rows if row(48) == "1") yield row(6)
//    parse(jack)

  }

  def parse(sentences: List[String]) {
    println("start generating!")

    val treeList = sentences.map(sentence => lp.parse(sentence.toLowerCase))

    // This is the normal
    // val subTrees = testTree.subTreeList().asScala

    treeList.map(tree => recursiveTraverse(tree, tree.depth()))

  }

  //print out structures when they are generated
  def recursiveTraverse(tree: Tree, depth: Int) {

    // if the "tree" passed in only contains
    // one level depth - meaning they are "leaves"
    // directly print them out, no loop
    // Finds the depth of the tree.  The depth is defined as the length
    // of the longest path from this node to a leaf node.  Leaf nodes
    // have depth zero.  POS tags have depth 1. Phrasal nodes have
    // depth >= 2.
    // This ending condition eliminates normal lexicons and leave
    // lexicons with their upper POS tag

    if (depth == 0) printToFile(tree.toString) //this happens at the leaf node (lexicons)

    for (i <- 1 to depth) {
      // all the children on the immediate level below
      for (i <- 1 to i) {

      }
      val listOfChildren = tree.children()

      //

    }

  }

  def recursiveGetChildren(tree: Tree, depth: Int) {

  }



  /**
   * On level 1 (with the POS tag node and leave node - word itself)
   * It's unlikely to have more than one branch
   * So no need to send it down ot appendChildrenVertical()
   * @param tree
   * @param level
   * @return
   */
  def appendChildrenHorizontal(tree: Tree, level: Int) = {
    if (level == 1) tree.children() else tree.children()
  }

  /**
   * use tree.siblings() method to generate all combination
   *
   * @param tree
   * @param columnSpan
   * @return
   */
  def appendChildrenVertical(tree: Tree, columnSpan: Int) = ???


  def printToFile(outputFile: String) {

  }

}
