package com.github.merelin.util.json

import com.github.merelin.util.json.JsonConverter._

import scala.xml.{Utility, XML}

object JsonConverterTest {
  def main(args: Array[String]) = {
    val xmlString =
      """<?xml version="1.0" encoding="UTF-8"?>
        |<theDavidBox>
        |  <request>
        |    <arg0>get_video_zoom</arg0>
        |    <arg1>get_language</arg1>
        |    <arg2>get_resolution</arg2>
        |    <arg3>get_display_mode</arg3>
        |    <arg4>get_audio_level</arg4>
        |    <module>setting</module>
        |  </request>
        |  <response>
        |    <videoZoom>actual size</videoZoom>
        |    <videoZoom>double size</videoZoom>
        |    <videoZoom>triple size</videoZoom>
        |  </response>
        |  <returnValue>0</returnValue>
        |</theDavidBox>
      """.stripMargin

    val json = xmlStringToJson(xmlString)

    val jsonFixed = indexedFieldsToArray(json = json, parent = "request", prefix = "arg")
    val jsonFixedToXmlString = jsonToXmlString(arrayToIndexedFields(json = jsonFixed, parent = "request", array = "arg"))

    require(Utility.trim(XML.loadString(xmlString)) == Utility.trim(XML.loadString(jsonFixedToXmlString)))

//    case class Document(theDavidBox: TheDavidBox)
//    case class TheDavidBox(request: Request, response: Response, returnValue: String)
//
//    case class Request(args: List[String], module: String)
//
////    case class Response(items: List[AnyRef])
//    case class Response(videoZoom: String)

//    println(Xml.toXml(jsonFixed))
//    println(fromJson(Xml.toJson(Xml.toXml(jsonFixed))))

//    val jsonAsString = fromJson(json)
//    println(jsonAsString)
//    println(writeTo[Document](Document(TheDavidBox(Request(List("get_video_zoom"), "setting"), Response("actual size"), "0"))))
//    println(readFrom[Document](StringInput(jsonAsString)))
//    println(Xml.toXml(json) == xml)
  }
}
