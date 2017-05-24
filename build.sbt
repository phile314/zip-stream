name := "zip-stream"

version := "0.1"

organization := "ISSKA"

scalaVersion := "2.11.8"

libraryDependencies ++= Seq(
/*  "com.github.nscala-time" %% "nscala-time" % "2.14.0",
  "joda-time" % "joda-time" % "2.9.4",
  "org.scodec" %% "scodec-core" % "1.10.3",
  "org.scodec" %% "scodec-bits" % "1.1.1",
  "org.log4s" %% "log4s" % "1.3.3",
  compilerPlugin("org.spire-math" %% "kind-projector" % "0.8.2"),
  "org.specs2" %% "specs2-core" % "3.8.5" % "test",
  "org.scalacheck" %% "scalacheck" % "1.13.4" % "test",
  "com.github.alexarchambault" %% "scalacheck-shapeless_1.13" % "1.1.3" % "test",
  // necessary to shut up some warnings
  // see https://issues.scala-lang.org/browse/SI-8978
  "com.google.code.findbugs" % "jsr305" % "3.0.2"*/
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
