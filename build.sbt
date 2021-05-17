import autoversion.Keys.{bugfixRegexes, defaultBump, minorRegexes}
import org.scalajs.linker.interface.OutputPatterns
import sbt.Keys.{libraryDependencies, name, publishMavenStyle, publishTo}
import sbtsonar.SonarPlugin.autoImport.sonarProperties

ThisBuild / scalaVersion := "2.13.5"
ThisBuild / versionScheme := Some("early-semver")

lazy val root = project.in(file(".")).
  aggregate(expressionsJVM, expressionsJS)

lazy val expressions = crossProject(JSPlatform, JVMPlatform).
  crossType(CrossType.Full).
  withoutSuffixFor(JVMPlatform).
  in(file(".")).
  settings(
    organization := "org.molgenis",
    name := "molgenis-expressions",
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
      "sonar.sources" -> "shared/src/main/scala",
      "sonar.tests" -> "shared/src/test/scala"
    )).
  jvmSettings(
    libraryDependencies += "org.scalatest" %%% "scalatest" % "3.2.7" % Test,
    libraryDependencies += "com.github.ben-manes.caffeine" % "caffeine" % "2.8.2",
    publishMavenStyle := true,
    publishM2Configuration := publishM2Configuration.value.withOverwrite(true),
    credentials += Credentials(Path.userHome / ".sbt" / ".credentials"),
    publishTo := {
      val nexus = "https://registry.molgenis.org/"
      if (isSnapshot.value)
        Some("Sonatype Nexus Repository Manager" at nexus + "repository/maven-snapshots")
      else
        Some("Sonatype Nexus Repository Manager" at nexus + "repository/maven-releases")
    },
  ).
  jsSettings(
    scalaJSLinkerConfig ~= {
      _.withModuleKind(ModuleKind.ESModule)
        .withOutputPatterns(OutputPatterns.fromJSFile("%s.mjs"))
    },
//    scalaJSLinkerConfig ~= { _.withSemantics(_.withStrictFloats(true)) }
    libraryDependencies += "io.github.cquiroz" %%% "scala-java-time-tzdb" % "2.0.0"
  )

pomExtra :=
  <developers>
    <developer>
      <name>Fleur Kelpin</name>
      <email>f.kelpin@umcg.nl</email>
    </developer>
  </developers>
    <scm>
      <connection>scm:git:git@github.com:molgenis/molgenis-expressions.git</connection>
      <developerConnection>scm:git:git@github.com:molgenis/molgenis-expressions.git</developerConnection>
      <url>git@github.com:molgenis/molgenis-expressions.git</url>
    </scm>


lazy val expressionsJVM = expressions.jvm
lazy val expressionsJS = expressions.js