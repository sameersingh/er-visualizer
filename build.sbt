name := "er-visualizer"

version := "1.0-SNAPSHOT"

libraryDependencies ++= Seq(
  jdbc,
  anorm,
  cache
)     

play.Project.playScalaSettings

resolvers += "Local Maven Repository" at "file:///"+Path.userHome+"/.m2/repository"

resolvers += Resolver.file("Local ivy2 repo", file(System.getProperty("user.home") + "/.ivy2/local"))(Resolver.ivyStylePatterns)

resolvers += Resolver.sonatypeRepo("public")

libraryDependencies += "edu.stanford.nlp" % "stanford-corenlp" % "3.3.1"

libraryDependencies += "org.mongodb" %% "casbah" % "2.7.2"

libraryDependencies += "multirexperiments" %% "multirexperiments" % "0.1"

libraryDependencies += "org.sameersingh.nlp_serde" % "nlp_serde" % "0.0.1"

javaOptions += "-Xmx16G"