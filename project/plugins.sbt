resolvers ++= Seq("Typesafe repository" at "http://repo.typesafe.com/typesafe/releases/")
  // The Play plugin
addSbtPlugin("com.typesafe.play" % "sbt-plugin" % "2.4.8")

addSbtPlugin("com.typesafe.sbt" % "sbt-native-packager" % "1.1.1")

addSbtPlugin("com.eed3si9n" % "sbt-assembly" % "0.14.3")