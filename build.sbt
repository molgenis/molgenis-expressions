import autoversion.Keys.{bugfixRegexes, defaultBump, minorRegexes}
import sbt.Keys.{libraryDependencies, name, publishMavenStyle, publishTo}
import sbtsonar.SonarPlugin.autoImport.sonarProperties

ThisBuild / scalaVersion := "2.13.5"

Global / excludeLintKeys += idePackagePrefix

lazy val root = project.in(file(".")).
  aggregate(expressionsJVM, expressionsJS)


lazy val expressions = crossProject(JSPlatform, JVMPlatform).
  crossType(CrossType.Pure).
  in(file("expressions")).
  settings(
    idePackagePrefix := Some("org.molgenis.expression"),
    organization := "org.molgenis",
    name := "molgenis-expressions",
    publishMavenStyle := true,
    publishM2Configuration := publishM2Configuration.value.withOverwrite(true),
    credentials += Credentials(Path.userHome / ".sbt" / ".credentials"),
    publishTo := {
      val nexus = "https://registry.molgenis.org/"
      if (isSnapshot.value)
        Some("Sonatype Nexus Repository Manager" at nexus + "repository/maven-snapshots")
      else
        Some("Sonatype Nexus Repository Manager"  at nexus + "repository/maven-releases")
    },
    majorRegexes := List(""".*BREAKING CHANGE:.*""".r),
    minorRegexes := List("""^feat:.*""".r),
    bugfixRegexes := List("""^fix:.*""".r),
    defaultBump := None,
    libraryDependencies += "com.lihaoyi" %%% "fastparse" % "2.3.2",
    libraryDependencies += "org.scalatest" %%% "scalatest" % "3.2.7" % Test,
    libraryDependencies += "io.github.cquiroz" %%% "scala-java-time" % "2.2.2",
    sonarProperties ++= Map(
      "sonar.host.url" -> "https://sonarcloud.io",
      "sonar.organization" -> "molgenis",
      "sonar.sources" -> "expressions/src/main/scala",
      "sonar.tests" -> "expressions/src/test/scala"
    )).
  jvmSettings(
    libraryDependencies += "org.scalatest" %%% "scalatest" % "3.2.7" % "test",
  ).
  jsSettings(
    scalaJSLinkerConfig ~= { _.withSemantics(_.withStrictFloats(true)) }
//    libraryDependencies += "io.github.cquiroz" %%% "scala-java-time-tzdb" % "2.0.0-RC3_2019a"
  )

lazy val expressionsJVM = expressions.jvm
lazy val expressionsJS = expressions.js