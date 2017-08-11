package com.github.merelin.util.json

import java.io.Writer

import org.json4s._
import org.json4s.native.JsonMethods._
import org.json4s.native.Serialization._

import scala.xml.{NodeSeq, XML}

object JsonConverter {
  implicit val fmt = formats(NoTypeHints)

  def toJson(s: String): JValue = parse(s)
  def fromJson(json: JValue): String = compact(render(json))

  def writeTo[A <: AnyRef](a: A)(implicit formats: Formats): String = write(a)
  def writeTo[A <: AnyRef, W <: Writer](a: A, w: W)(implicit formats: Formats): W = write(a, w)
  def readFrom[A <: AnyRef](jsonInput: JsonInput)(implicit formats: Formats, manifest: Manifest[A]): A = read(jsonInput)

  def xmlToJson(xml: NodeSeq): JValue = Xml.toJson(xml)
  def xmlStringToJson(s: String): JValue = xmlToJson(XML.loadString(s))

  def jsonToXml(json: JValue): NodeSeq = Xml.toXml(json)
  def jsonToXmlString(json: JValue): String = jsonToXml(json).toString

  def indexedFieldsToArray(json: JValue, parent: String, prefix: String): JValue = {
    val pattern = s"${prefix}[0-9]+"

    // Collect indexed fields values
    val values = (json \\ parent).filterField { case (name, _) => name.matches(pattern) }.map(_._2)

    // Replace indexed fields with a JArray
    json.transformField {
      case (`parent`, value) => parent -> (JObject(JField(prefix, JArray(values))) merge value)
    }.removeField { case (name, _) => name.matches(pattern) }
  }

  def arrayToIndexedFields(json: JValue, parent: String, array: String): JValue = {
    // Replace JArray values with indexed fields
    json.transformField {
      case (`parent`, value) =>
        parent -> (value \ array).children.zipWithIndex.reverse.fold(value -> 0) {
          case ((v, _), (child, i)) => ((JObject(JField(s"${array}${i}", child))) merge v) -> 0
        }._1
    }.removeField { case (name, _) => name.matches(array) }
  }
}
