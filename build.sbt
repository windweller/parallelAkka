import AssemblyKeys._

name := """parallel-Akka"""

version := "1.0"

scalaVersion := "2.11.2"

mainClass in (Compile, run) := Some("DataTransform.Entry")
//mainClass in (Compile, run) := Some("classification.Main")

resolvers ++= Seq(
  "Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/",
  "IESL Release" at "http://dev-iesl.cs.umass.edu/nexus/content/groups/public"
)

val akkaV = "2.3.6" //2.2.1

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % akkaV,
  "com.typesafe.akka" %% "akka-testkit" % akkaV % "test",
  "com.typesafe.akka" %% "akka-slf4j" % akkaV,
  "com.typesafe.akka" %% "akka-stream-experimental" % "1.0-M4",
  "ch.qos.logback" % "logback-classic" % "1.1.2",
  "com.github.scala-incubator.io" %% "scala-io-core" % "0.4.3",
  "com.github.scala-incubator.io" %% "scala-io-file" % "0.4.3",
  "org.scalatest" %% "scalatest" % "2.2.1" % "test",
  "com.typesafe.slick" %% "slick" % "2.1.0",
  "mysql" % "mysql-connector-java" % "5.1.12",
  "org.scalaj" %% "scalaj-http" % "0.3.15",
  "com.github.tototoshi" %% "scala-csv" % "1.0.0",
  "commons-net" % "commons-net" % "3.3",
  "edu.stanford.nlp" % "stanford-corenlp" % "3.3.1",
  "commons-cli" % "commons-cli" % "1.2",
  "org.ccil.cowan.tagsoup" % "tagsoup" % "1.2.1",
  "org.apache.commons" % "commons-csv" % "1.1",
  "cc.mallet" % "mallet" % "2.0.7-RC2",
  "cc.factorie" %% "factorie" % "1.1",
  "com.bizo" % "mighty-csv_2.10" % "0.2",
//Clear NLP - core
  "edu.emory.clir" % "clearnlp" % "3.0.1",
  "edu.emory.clir" % "clearnlp-dictionary" % "3.0",
  //Clear NLP - English Dependency Parsing
  "edu.emory.clir" % "clearnlp-general-en-dep" % "3.1",
  //POS Tagging
  "edu.emory.clir" % "clearnlp-general-en-pos" % "3.1",
  //machine learning
  "org.scalanlp" % "nak" % "1.2.1",
  "tw.edu.ntu.csie" % "libsvm" % "3.17"
)



unmanagedJars in Compile := (baseDirectory.value ** "*.jar").classpath

mainClass in assembly := Some("TwitterLinear.Entry")

jarName in assembly := "FuturePastClassifier_beta_0.9.jar"

assemblySettings