name := "er-visualizer"

version := "1.0-SNAPSHOT"

libraryDependencies ++= Seq(
  jdbc,
  anorm,
  cache
)     

play.Project.playScalaSettings

libraryDependencies += "edu.stanford.nlp" % "stanford-corenlp" % "3.3.1"
