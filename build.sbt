name := "zip-stream"

version := "0.1"

organization := "314.ch"

description := "Streaming zip library for scalaz-stream."

licenses += ("BSD3", url("https://opensource.org/licenses/BSD-3-Clause"))

homepage := Some(url("https://github.com/phile314/zip-stream"))

scmInfo := Some(
  ScmInfo(
    url("https://github.com/phile314/zip-stream"),
    "scm:git@github.com:phile314/zip-stream.git"
  )
)

developers := List(
  Developer(
    id    = "phile314",
    name  = "Philipp Hausmann",
    email = "ph_git@314.ch",
    url   = url("https://github.com/phile314")
  )
)

publishMavenStyle := true

publishTo := {
  val nexus = "https://oss.sonatype.org/"
  if (isSnapshot.value)
    Some("snapshots" at nexus + "content/repositories/snapshots")
  else
    Some("releases"  at nexus + "service/local/staging/deploy/maven2")
}


scalaVersion := "2.11.8"

libraryDependencies ++= Seq(
  "com.github.nscala-time" %% "nscala-time" % "2.14.0",
  "joda-time" % "joda-time" % "2.9.4",
  "org.scodec" %% "scodec-core" % "1.10.3",
  "org.scodec" %% "scodec-bits" % "1.1.2",
  "org.scalaz" %% "scalaz-core" % "7.2.6",
  "org.scalaz.stream" %% "scalaz-stream" % "0.8.5a"
/*
  "org.specs2" %% "specs2-core" % "3.8.5" % "test",
  "org.scalacheck" %% "scalacheck" % "1.13.4" % "test",
  "com.github.alexarchambault" %% "scalacheck-shapeless_1.13" % "1.1.3" % "test",*/
)

scalacOptions ++= Seq(
  "-target:jvm-1.8",
  "-unchecked",
  "-deprecation",
  "-Xfuture",
  "-Yno-adapted-args",
  "-Ywarn-dead-code",
  "-Ywarn-numeric-widen",
  "-Ywarn-value-discard",
  "-Ywarn-unused",
  "-feature",
  "-Xfatal-warnings",
  "-language:higherKinds")

// test error reporting
scalacOptions in Test ++= Seq("-Yrangepos")
