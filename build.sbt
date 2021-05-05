import sbtsonar.SonarPlugin.autoImport.sonarProperties

organization := "org.molgenis"
name := "molgenis-expressions"

version := "0.1-SNAPSHOT"
isSnapshot := true

scalaVersion := "2.13.5"

idePackagePrefix := Some("org.molgenis.expression")

// publish to registry.molgenis.org
credentials += Credentials(Path.userHome / ".sbt" / ".credentials")
publishTo := {
  val nexus = "https://registry.molgenis.org/"
  if (isSnapshot.value)
    Some("Sonatype Nexus Repository Manager" at nexus + "repository/maven-snapshots")
  else
    Some("Sonatype Nexus Repository Manager"  at nexus + "repository/maven-releases")
}

publishMavenStyle := true
publishM2Configuration := publishM2Configuration.value.withOverwrite(true)

libraryDependencies += "org.scala-lang.modules" %% "scala-parser-combinators" % "1.1.2"
libraryDependencies += "org.scalatest" %% "scalatest" % "3.2.7" % "test"


sonarProperties ++= Map(
  "sonar.host.url" -> "https://sonarcloud.io",
  "sonar.organization" -> "molgenis"
)