import AssemblyKeys._

name := """parallel-Akka"""

version := "1.0"

scalaVersion := "2.10.2"

//mainClass in (Compile, run) := Some("clearNLP.DemoNLPDecode")
mainClass in (Compile, run) := Some("blogParallel.Entry")

val buildSettings = Defaults.defaultSettings ++ Seq(
  javaOptions += "-Xmx2G -Xms4G"
)

resolvers += "Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % "2.2.1",
  "com.typesafe.akka" %% "akka-testkit" % "2.2.1",
  "com.typesafe.akka" %% "akka-slf4j" % "2.3.2",
  "com.github.scala-incubator.io" %% "scala-io-core" % "0.4.3",
  "com.github.scala-incubator.io" %% "scala-io-file" % "0.4.3",
  "org.scalatest" %% "scalatest" % "1.9.1" % "test",
  "com.typesafe.slick" %% "slick" % "2.0.1",
  "mysql" % "mysql-connector-java" % "5.1.12",
  "org.scalaj" %% "scalaj-http" % "0.3.15",
  "com.github.tototoshi" %% "scala-csv" % "1.0.0",
  "commons-net" % "commons-net" % "3.3",
  "edu.stanford.nlp" % "stanford-corenlp" % "3.3.1",
  "commons-cli" % "commons-cli" % "1.2",
  //Clear NLP - core
  "com.clearnlp" % "clearnlp" % "2.0.2",
  "com.clearnlp" % "clearnlp-dictionary" % "1.0",
  //Clear NLP - English Dependency Parsing
  "com.clearnlp" % "clearnlp-general-en-dep" % "1.2",
  //POS Tagging
  "com.clearnlp" % "clearnlp-general-en-pos" % "1.1",
  //Semantic Role Labeling
  "com.clearnlp" % "clearnlp-general-en-srl" % "1.1"
)


unmanagedJars in Compile := (baseDirectory.value ** "*.jar").classpath

mainClass in assembly := Some("TwitterLinear.Entry")

jarName in assembly := "FuturePastClassifier_beta_0.9.jar"

assemblySettings