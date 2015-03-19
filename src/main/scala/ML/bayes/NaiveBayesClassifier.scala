package ML.bayes

import DataTransform.FileOp._
import DataTransform.Utils._

object NaiveBayesClassifier extends App {

  type Vector[T] = List[T]
  type Matrix[T] = List[List[T]]
  type Label = String
  type ID = String


  //import CSV file
  val data = new CSV("E:\\Allen\\Linguistics\\JasonNewMturkTrainSet80WithSentence.csv", header = true).data

  val labels = data.map(r => r(0) -> r(r.length - 1)).toMap //Y labels

  val featureMap: Map[(ID, Label), Vector[String]] = data.map(r =>
    (r(0), r(r.length - 1)) -> dropEle[String](Set(0,1, r.length - 1), r)).toMap

//  val featureVariableLabel = data.groupBy()

  val labelCate = labels.values.filter(x => x.trim != "").toSet

  if (labelCate.size > 2) {println("Label category can't be larger than 2"); println(labelCate); sys.exit(0);}

  //In order to classify, we need to know a few variables first
  //first, priors of all Y

  val priorY: Map[Label, Float] = labelCate.map(label => label -> (labels.values.count(cl => cl == label) / labels.values.size.toFloat)).toMap

  println("Priors of Labels are: " + priorY)

  //partition on label
//  val xMatrixConditionedOnY = labelCate.map(label => label -> ())

}
