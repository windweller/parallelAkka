package DataTransform

import DataTransform.FileOp._
import Transform._
import VectorConverter._

object Entry extends App {

  //xmlTransform(new CSV("E:\\Allen\\R\\naacl2015\\subtree\\MTurkAllSentences_NANFuture.csv", true), 4,"E:\\Allen\\R\\naacl2015\\subtree\\MTUrkAllSentences_NANFuture_XML.xml")

//  varroTransform("E:\\Allen\\R\\naacl2015\\subtree\\MTUrkAllSentences_NANFuture_XML.xml",
//    "E:\\Allen\\R\\naacl2015\\subtree\\MTUrkAllSentences_NANFuture_Varro.xml", 0, hasContinuedPart = false, isCountinuedPart = false)

  val future = Evaluation.generateXMLTree("E:\\Allen\\R\\naacl2015\\subtree\\mTurkAllSentencesFuture.xml")

//  val notFuture = Evaluation.generateXMLTree("E:\\Allen\\R\\naacl2015\\subtree\\mTurkAllSentencesNANFuture.xml")

//  val combined = Evaluation.generateXMLTree("E:\\Allen\\Linguistics\\mTurkFutureNotFutureCombinedOrderedSubtrees.xml")

//  combined.saveSVMLightMatrix("E:\\Allen\\Linguistics\\combinedSVMMatrix.txt", 214, svmForm = false)

//  Evaluation.printDIFF("E:\\Allen\\R\\naacl2015\\subtree\\mTurkAllSentencesDiff.csv", Evaluation.compareDIFF(future, notFuture))

  future.saveLDASentenceMap("E:\\Allen\\R\\naacl2015\\subtree\\sentencesScala", Some("E:\\Allen\\R\\naacl2015\\subtree\\mTurkAllSentencesDiff.csv"), Some(0))


//  val joinFile = JoinFile(CSV("E:\\Allen\\Linguistics\\JasonNewDataRawSentence.csv", true), TabFile("E:\\Allen\\Linguistics\\JasonNewMturkTestSet20.txt"), Some(0))
//  joinFile.save("E:\\Allen\\Linguistics\\JasonNewMturkTestSet20withSentence.csv")

}