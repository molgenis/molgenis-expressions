import autoversion.Keys.{bugfixRegexes, defaultBump, minorRegexes}
import org.scalajs.linker.interface.OutputPatterns
import sbt.Keys.{libraryDependencies, name, publishMavenStyle, publishTo}
import sbtsonar.SonarPlugin.autoImport.sonarProperties
import sys.process._
import ReleaseTransformations._

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
    minorRegexes := List("""^feat(\(.*\))?:.*""".r),
    bugfixRegexes := List("""^fix(\(.*\))?:.*""".r),
    defaultBump := None,
    libraryDependencies += "com.lihaoyi" %%% "fastparse" % "2.3.2",
    libraryDependencies += "org.scalatest" %%% "scalatest" % "3.2.7" % Test,
    libraryDependencies += "io.github.cquiroz" %%% "scala-java-time" % "2.2.2",
    sonarProperties ++= Map(
      "sonar.host.url" -> "https://sonarcloud.io",
      "sonar.organization" -> "molgenis",
      "sonar.sources" -> "shared/src/main/scala",
      "sonar.tests" -> "shared/src/test/scala"
    ),
    releaseProcess := Seq[ReleaseStep](
      checkSnapshotDependencies,
      inquireVersions,
      runClean,
      runTest,
      setReleaseVersion,
      setVersionNpm,
      commitReleaseVersion,
      tagRelease,
      publishArtifacts,
      setNextVersion,
      commitNextVersion,
      pushChanges
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
    scmInfo := Some(ScmInfo(new URL("https://github.com/molgenis/molgenis-expressions"),
      "https://github.com/molgenis/molgenis-expressions.git"))
  )

lazy val setVersionNpm = ReleaseStep(action = st => {
  val extracted = Project.extract(st)
  val version = extracted.get(Keys.version)
  val shell: Seq[String] = Seq("sh", "-c")
  val npmSetVersion: Seq[String] = shell :+ s"npm version ${version} --no-git-tag-version"
  val gitAddNpmVersion: Seq[String] = shell :+ "git add package.json"
  if ((npmSetVersion #&& gitAddNpmVersion).! != 0) {
    throw new IllegalStateException("Failed to publish")
  }
  st
})

lazy val expressionsJVM = expressions.jvm
lazy val expressionsJS = expressions.js