name := "mma-aggregator"

version := "0.1-SNAPSHOT"

libraryDependencies ++= Seq(
  jdbc,
  anorm,
  cache
)

libraryDependencies += "org.postgresql" % "postgresql" % "9.4-1201-jdbc41"

play.Project.playScalaSettings
