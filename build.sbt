organization := "org.molgenis"
name := "molgenis-expressions"

version := "0.1-SNAPSHOT"
isSnapshot := true

scalaVersion := "2.13.5"

idePackagePrefix := Some("org.molgenis.expression")

publishMavenStyle := true
publishM2Configuration := publishM2Configuration.value.withOverwrite(true)

libraryDependencies += "org.scala-lang.modules" %% "scala-parser-combinators" % "1.1.2"
libraryDependencies += "org.scalatest" %% "scalatest" % "3.2.7" % "test"
