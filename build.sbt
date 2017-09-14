val assemblyName = "geocoding"

scalacOptions ++= Seq(
  "-deprecation",
  "-encoding", "UTF-8", // yes, this is 2 args
  "-feature",
  "-unchecked",
  "-Xfatal-warnings",
  "-Xlint",
  "-Yno-adapted-args",
  "-Ywarn-numeric-widen",
  "-Ywarn-value-discard",
  "-Ywarn-dead-code",
  "-Xfuture"
)

val commonDependencies = Seq(
  "com.typesafe" % "config" % "1.3.1",
  "org.slf4j" % "slf4j-api" % "1.7.5",
  "org.slf4j" % "slf4j-log4j12" % "1.7.5",
  "org.elasticsearch.client" % "rest" % "5.5.2",
  "org.json4s" %% "json4s-jackson" % "3.5.3",
  "com.sparkjava" % "spark-core" % "2.6.0",
  "org.specs2" %% "specs2-core" % "3.9.1" % "test",
  "com.github.docker-java" % "docker-java" % "3.0.13" % "test"
)

val scalaxbDependencies = Seq(
  "org.scala-lang.modules" %% "scala-xml" % "1.0.6",
  "org.scala-lang.modules" %% "scala-parser-combinators" % "1.0.6",
  "net.databinder.dispatch" %% "dispatch-core" % "0.13.1"
)

lazy val root = (project in file(".")).
  enablePlugins(ScalaxbPlugin)
  .settings(
    name := "geocoding",
    organization := "it.teamdigitale",
    scalaVersion := "2.11.9",
    version := "1.0",
    libraryDependencies ++= commonDependencies ++ scalaxbDependencies)
  .settings(

    scalaxbPackageName in (Compile, scalaxb) := "generated",
    scalaxbDispatchVersion in (Compile, scalaxb) := "0.13.1",
    //output dir, generate this directory before to run sbt clean compile
    sourceManaged in (Compile, scalaxb) := (sourceDirectory in Compile).value / "sbt-scalaxb"
  )
