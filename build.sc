import mill._
import mill.define._
import scalalib._

object CommonUtils extends ScalaModule {

  def scalaVersion = "3.2.1"

  override def ivyDeps = Agg(
    ivy"org.scala-lang.modules::scala-xml:2.1.0",
    ivy"org.json4s::json4s-ast:4.0.6",
    ivy"org.json4s::json4s-core:4.0.6",
    ivy"org.json4s::json4s-native:4.0.6",
    ivy"org.json4s::json4s-ext:4.0.6",
    ivy"org.json4s::json4s-xml:4.0.6",
    ivy"com.ibm.icu:icu4j:72.1",
    ivy"com.h2database:h2:2.1.214",
    ivy"org.postgresql:postgresql:42.3.1",
    ivy"com.zaxxer:HikariCP:5.0.1",
    ivy"org.codehaus.jettison:jettison:1.5.2",
    ivy"com.thoughtworks.xstream:xstream:1.4.19",
    ivy"org.slf4j:slf4j-api:2.0.5",
    ivy"ch.qos.logback:logback-classic:1.4.5"
  )

  object test extends Tests {
    override def ivyDeps = Agg(
      ivy"org.scalactic::scalactic:3.2.14",
      ivy"org.scalatest::scalatest:3.2.14"
    )

    override def testFramework: Target[String] = "org.scalatest.tools.Framework"
  }
}
