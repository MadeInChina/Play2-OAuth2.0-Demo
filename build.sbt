import sbt.Keys._

name := "play2-oauth2-demo"

organization in ThisBuild := "com.hrw"

version in ThisBuild := "0.1-SNAPSHOT"

scalaVersion in ThisBuild := "2.11.6"

scalacOptions in ThisBuild := Seq("-unchecked", "-deprecation", "-encoding", "utf8")

resolvers += "cloudera" at "https://repository.cloudera.com/artifactory/cloudera-repos/"

run := {
  (run in `demo-application` in Compile).evaluated
}

lazy val versionOfJson4s = "3.2.11"

lazy val versionOfAkka = "2.3.11"

lazy val `demo-application` = project
  .enablePlugins(PlayScala)
  .dependsOn(`oauth2-service`)
  .settings(
    name := "demo-application",
    ivyScala := ivyScala.value map {
      _.copy(overrideScalaVersion = true)
    },
    routesGenerator := InjectedRoutesGenerator,
    libraryDependencies ++= logs,
    libraryDependencies ++= test,
    libraryDependencies ++= json,
    libraryDependencies ++= mongodb,
    libraryDependencies ++= guice,
    libraryDependencies ++= redis,
    libraryDependencies ++= akka
  )


lazy val `oauth2-service` = project
  .settings(
    name := "oauth2-service",
    ivyScala := ivyScala.value map {
      _.copy(overrideScalaVersion = true)
    },
    libraryDependencies ++= logs,
    libraryDependencies ++= test,
    libraryDependencies ++= json,
    libraryDependencies ++= mongodb,
    libraryDependencies ++= guice,
    libraryDependencies ++= akka,
    libraryDependencies ++= redis,
    libraryDependencies ++= oauth2
  )

lazy val redis = Seq(
  "net.debasishg" %% "redisclient" % "3.0"
)

lazy val oauth2 = Seq(
  "com.nulab-inc" %% "play2-oauth2-provider" % "0.15.0"
)

lazy val logs = Seq(
  "org.slf4j" % "jul-to-slf4j" % "1.7.7"
)

lazy val test = Seq(
  //  "com.novocode" % "junit-interface" % "0.11" % "test",
  "org.mockito" % "mockito-core" % "1.9.5" % "test",
  "com.github.simplyscala" %% "scalatest-embedmongo" % "0.2.2" % "test",
  "org.scalatest" %% "scalatest" % "2.2.5" % "test",
  "com.github.kstyrc" % "embedded-redis" % "0.6" % "test"
)

lazy val mongodb = Seq(
  "com.novus" %% "salat" % "1.9.9"
)

lazy val json = Seq(
  "org.json4s" %% "json4s-jackson" % versionOfJson4s withSources(),
  "org.json4s" %% "json4s-ext" % versionOfJson4s withSources(),
  "com.jayway.restassured" % "json-path" % "2.4.0"
)

lazy val akka = Seq(
  "com.typesafe.akka" %% "akka-actor" % versionOfAkka,
  "com.typesafe.akka" %% "akka-cluster" % versionOfAkka,
  "com.typesafe.akka" %% "akka-kernel" % versionOfAkka,
  "com.typesafe.akka" %% "akka-slf4j" % versionOfAkka,
  "com.typesafe.akka" %% "akka-contrib" % versionOfAkka,
  "com.typesafe.akka" %% "akka-testkit" % versionOfAkka
)

lazy val guice = Seq(
  "net.codingwell" %% "scala-guice" % "4.0.0",
  "com.google.inject" % "guice" % "4.0"
)


