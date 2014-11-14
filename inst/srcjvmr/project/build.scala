
import sbtassembly.Plugin._
import AssemblyKeys._

import sbt._
import sbt.Keys._
import sbtrelease.ReleasePlugin._

object ScalaBuild extends sbt.Build {
  // Need quite a few things from here - http://www.scala-sbt.org/0.13/docs/Configuring-Scala.html
  lazy val root =
    project(id = "jvmr-scala",
            settings = Seq(
              libraryDependencies <++= scalaVersion (v => Seq(
                "joda-time" % "joda-time" % "2.1",
                "org.joda" % "joda-convert" % "1.2"
              ) ++ Shared.testDeps(v)),
              libraryDependencies += "org.scala-lang" % "scala-library" % scalaVersion.value,
              libraryDependencies += "org.scala-lang" % "scala-reflect" % scalaVersion.value,
              libraryDependencies += "org.scala-lang" % "scala-compiler" % scalaVersion.value,
              libraryDependencies += "org.scala-lang" % "scala-compiler" % scalaVersion.value % "scala-tool",
              libraryDependencies += "org.scala-lang" % "jline" % scalaVersion.value,
            // https://github.com/sbt/sbt-assembly/issues/92
              mergeStrategy in assembly <<= (mergeStrategy in assembly) { (old) =>
                {
                  case PathList("org", "fusesource", "jansi", xs @ _*) => MergeStrategy.first
                  case x => old(x)
                }
              },
              publishArtifact := false
            ),
            base = file(".")) 


  def project(id: String, base: File, settings: Seq[Project.Setting[_]] = Nil) =
    Project(id = id,
            base = base,
            settings = Project.defaultSettings ++ Shared.settings ++ releaseSettings ++ assemblySettings ++ settings)
}

object Shared {
  def testDeps(version: String, conf: String = "test") = {
    val specs2 = if (version.startsWith("2.1"))
      "org.specs2" %% "specs2" % "2.4.1"
    else if (version.startsWith("2.9.3"))
      "org.specs2" %% "specs2" % "1.12.4.1"
    else
      "org.specs2" %% "specs2" % "1.12.4"

    val scalacheck = if (version.startsWith("2.9"))
      "org.scalacheck" %% "scalacheck" % "1.10.1"
    else
      "org.scalacheck" %% "scalacheck" % "1.11.5"

    Seq(
      specs2 % conf,
      scalacheck % conf,
      "junit" % "junit" % "4.11" % conf
    )
  }

  val settings = Seq(
    organization := "org.jvmr-scala",
    publishMavenStyle := true,
    publishArtifact in Test := false,
    pomIncludeRepository := { x => false },
    scalaVersion := "2.10.4",
    version := "1.0.0-SNAPSHOT",
    crossScalaVersions := Seq("2.9.2", "2.9.3", "2.10.4"),
    scalacOptions := Seq("-deprecation", "-unchecked"), // , "-Xexperimental"),
    shellPrompt := { (state: State) => "[%s]$ " format(Project.extract(state).currentProject.id) },
    resolvers ++= Seq(
      "Sonatype OSS Releases" at "http://oss.sonatype.org/content/repositories/releases/",
      "Sonatype OSS Snapshots" at "http://oss.sonatype.org/content/repositories/snapshots/"
    ),
    publishTo <<= (version) { version: String =>
      val nexus = "https://oss.sonatype.org/"
      if (version.trim.endsWith("SNAPSHOT"))
        Some("snapshots" at nexus + "content/repositories/snapshots")
      else
        Some("releases" at nexus + "service/local/staging/deploy/maven2")
    },
    //credentials += Credentials(Path.userHome / ".ivy2" / ".credentials"),
    compile <<= (compile in Compile) dependsOn (compile in Test)
  )
}
