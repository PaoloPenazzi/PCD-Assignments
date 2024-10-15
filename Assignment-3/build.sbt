ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "3.3.0"

lazy val root = (project in file("."))
  .settings(
    name := "Assignment-3"
  )

lazy val akkaVersion = "2.8.2"
lazy val akkaGroup = "com.typesafe.akka"
libraryDependencies ++= Seq(
  akkaGroup %% "akka-actor-typed" % akkaVersion, // For standard log configuration
  akkaGroup %% "akka-actor-testkit-typed" % akkaVersion % Test, // For testing
  akkaGroup %% "akka-cluster-typed" % akkaVersion, // For cluster configuration (akka cluster module)
  akkaGroup %% "akka-remote" % akkaVersion, // For remote configuration (akka remote module)
  akkaGroup %% "akka-serialization-jackson" % akkaVersion, // For serialization configuration (akka serialization module)
  "ch.qos.logback" % "logback-classic" % "1.5.11",
  "org.scalatest" %% "scalatest" % "3.2.16" % Test
)
