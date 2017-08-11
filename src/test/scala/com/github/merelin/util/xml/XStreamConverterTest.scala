package com.github.merelin.util.xml

import com.thoughtworks.xstream.XStream

import scala.collection.mutable.{ArrayBuffer, ListBuffer}
import scala.reflect.ClassTag
import scala.xml.{Utility, XML}

object XStreamConverterTest {
  implicit val xstream = XStreamConverter(new XStream)

  def test() {
    List(
      (new ArrayBuffer[Int] ++= List(1, 2, 3)) -> """
                                                    |<arrayBuffer>
                                                    |  <int>1</int>
                                                    |  <int>2</int>
                                                    |  <int>3</int>
                                                    |</arrayBuffer>
                                                    |""".stripMargin,
      (new ListBuffer[Int] ++= List(1, 2, 3)) -> """
                                                   |<listBuffer>
                                                   |  <int>1</int>
                                                   |  <int>2</int>
                                                   |  <int>3</int>
                                                   |</listBuffer>
                                                   |""".stripMargin,
      (Seq() ++ List(1, 2, 3)) -> """
                                    |<list>
                                    |  <int>1</int>
                                    |  <int>2</int>
                                    |  <int>3</int>
                                    |</list>
                                    |""".stripMargin,
      (List() ++ List(1, 2, 3)) -> """
                                     |<list>
                                     |  <int>1</int>
                                     |  <int>2</int>
                                     |  <int>3</int>
                                     |</list>
                                     |""".stripMargin,
      (Set() ++ List(1, 2, 3)) -> """
                                    |<scala.collection.immutable.Set_-Set3>
                                    |  <int>1</int>
                                    |  <int>2</int>
                                    |  <int>3</int>
                                    |</scala.collection.immutable.Set_-Set3>
                                    |""".stripMargin,
      (Map() ++ List(1 -> 1, 2 -> 2, 3 -> 3)) -> """
                                                   |<scala.collection.immutable.Map_-Map3>
                                                   |  <tuple>
                                                   |    <int>1</int>
                                                   |    <int>1</int>
                                                   |  </tuple>
                                                   |  <tuple>
                                                   |    <int>2</int>
                                                   |    <int>2</int>
                                                   |  </tuple>
                                                   |  <tuple>
                                                   |    <int>3</int>
                                                   |    <int>3</int>
                                                   |  </tuple>
                                                   |</scala.collection.immutable.Map_-Map3>
                                                   |""".stripMargin,
      (1, 2, 3, 4, 5) -> """
                           |<tuple>
                           |  <int>1</int>
                           |  <int>2</int>
                           |  <int>3</int>
                           |  <int>4</int>
                           |  <int>5</int>
                           |</tuple>
                         """.stripMargin
    ).foreach {case (t, e) => testType(t, e)}
  }

  def testType[T](value: T, expected: String)(implicit stream: XStream, manifest: ClassTag[T]) {
    val xmlString = stream.toXML(value)
    val xml = Utility.trim(XML.loadString(xmlString))
    println("Test serialization/deserialization for the type: " + value.getClass.getName)

    require(
      xml == Utility.trim(XML.loadString(expected)),
      s"Actual \n${xml}\n does not equal to expected \n${expected}"
    )
    require(
      stream.fromXML(xmlString) == value,
      s"Actual \n${stream.fromXML(xmlString)}\n does not equal to expected \n${value}"
    )
  }

  def main(args: Array[String]): Unit = {
    test()

    case class Topping(name: String)

    case class Pizza(crustSize: Int, crustType: String, toppings: List[Topping] = Nil) {
      def +(t: Topping): Pizza = copy(toppings = toppings :+ t)
    }

    var p = Pizza(14, "Thin")
    p += Topping("cheese")
    p += Topping("sausage")

    xstream.alias("topping", classOf[Topping])
    xstream.alias("pizza", classOf[Pizza])

    testType(
      value = p,
      expected =
        """
          |<pizza>
          |  <crustSize>14</crustSize>
          |  <crustType>Thin</crustType>
          |  <toppings>
          |    <topping>
          |      <name>cheese</name>
          |    </topping>
          |    <topping>
          |      <name>sausage</name>
          |    </topping>
          |  </toppings>
          |</pizza>
        """.stripMargin
    )
  }
}
