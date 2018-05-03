name := "play2-oauth2-demo"

organization := "com.hrw"

version := "0.1-SNAPSHOT"

scalaVersion := "2.12.6"

scalacOptions := Seq("-unchecked", "-deprecation", "-encoding", "utf8")

resolvers += "cloudera" at "https://repository.cloudera.com/artifactory/cloudera-repos/"

resolvers += "scalaz-bintray" at "https://dl.bintray.com/scalaz/releases"

resolvers += "Akka Snapshot Repository" at "http://repo.akka.io/snapshots/"

run := {
  (run in `demo-application` in Compile).evaluated
}

lazy val versionOfJson4s = "3.4.2"

lazy val versionOfAkka = " 2.5.12"

lazy val `demo-application` = project
  .enablePlugins(PlayScala)
  .dependsOn(`oauth2-service`)
  .settings(
    name := "demo-application",
    routesGenerator := InjectedRoutesGenerator,
    libraryDependencies ++= logs,
    libraryDependencies ++= test,
    libraryDependencies ++= mongodb,
    libraryDependencies += guice,
    libraryDependencies ++= redis,
    libraryDependencies ++= json
  )


lazy val `oauth2-service` = project
  .settings(
    name := "oauth2-service",
    libraryDependencies ++= logs,
    libraryDependencies ++= test,
    libraryDependencies ++= mongodb,
    libraryDependencies += guice,
    libraryDependencies ++= redis,
    libraryDependencies ++= oauth2,
    libraryDependencies ++= json,
    libraryDependencies += "com.typesafe" % "config" % "1.3.1"
  )

lazy val redis = Seq(
  "net.debasishg" %% "redisclient" % "3.6"
)



lazy val oauth2 = Seq(
  "com.nulab-inc" %% "scala-oauth2-core" % "1.3.0",
  "com.nulab-inc" %% "play2-oauth2-provider" % "1.3.0"
)

lazy val logs = Seq(
  "org.slf4j" % "jul-to-slf4j" % "1.7.7"
)

lazy val test = Seq(
  //  "com.novocode" % "junit-interface" % "0.11" % "test",
  "org.mockito" % "mockito-core" % "2.18.3" % "test",
  "com.github.simplyscala" %% "scalatest-embedmongo" % "0.2.4" % "test",
  "org.scalatest" %% "scalatest" % "3.0.5" % "test",
  "com.github.kstyrc" % "embedded-redis" % "0.6" % "test"
)

lazy val mongodb = Seq(
  "com.github.salat" %% "salat" % "1.11.2"
)

lazy val json = Seq(
  "org.json4s" %% "json4s-jackson" % versionOfJson4s withSources(),
  "org.json4s" %% "json4s-ext" % versionOfJson4s withSources(),
  "com.jayway.restassured" % "json-path" % "2.4.0"
)

