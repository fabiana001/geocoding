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
  "com.typesafe.play" %% "play" % "2.6.2",
  "org.slf4j" % "slf4j-api" % "1.7.5",
  "org.slf4j" % "slf4j-log4j12" % "1.7.5",
  "org.elasticsearch.client" % "rest" % "5.5.2",
  "org.json4s" %% "json4s-jackson" % "3.5.3",
  "org.specs2" %% "specs2-core" % "3.9.1" % "test",
  "com.github.docker-java" % "docker-java" % "3.0.13"// % "test"
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
    scalaVersion := "2.12.0",
    version := "1.0",
    libraryDependencies ++= commonDependencies ++ scalaxbDependencies)
    .settings(
    //sbt coname := "geocoding",
    //libraryDependencies ++= commonDependencies ++ scalaxbDependencies,
    scalaxbPackageName in (Compile, scalaxb) := "generated",
    //scalaxbAutoPackages in (Compile, scalaxb) := true,
    scalaxbDispatchVersion in (Compile, scalaxb) := "0.13.1",
    scalaxbXsdSource in (Compile, scalaxb) := file("/src/main/scala/xsd/BlocchiImpresa.xsd"),
    sourceManaged in (Compile, scalaxb) := file("src/main/scala/xsd")
    //sourceManaged in (Compile, scalaxb) := (sourceDirectory in Compile).value / "xsd"

    )
