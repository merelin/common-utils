name := "common-utils"

version := "1.0"

scalaVersion := "2.12.3"

resolvers ++= Seq("default" at "http://nexus.global.trafigura.com:8081/nexus/content/groups/mirror/")

libraryDependencies ++= Seq(
  "org.scala-lang.modules" %% "scala-xml" % "1.0.6",
  "org.codehaus.jettison" % "jettison" % "1.3.7",
  "com.thoughtworks.xstream" % "xstream" % "1.4.7",
  "org.json4s" %% "json4s-ast" % "3.5.3",
  "org.json4s" %% "json4s-core" % "3.5.3",
  "org.json4s" %% "json4s-native" % "3.5.3",
  "org.json4s" %% "json4s-ext" % "3.5.3",
  "com.h2database" % "h2" % "1.4.193",
  "com.zaxxer" % "HikariCP" % "2.6.3",
  "org.slf4j" % "slf4j-api" % "1.7.25",
  "ch.qos.logback" % "logback-classic" % "1.2.3"
)
