package com.github.merelin.util.finance.cbr

import com.github.merelin.util.json.JsonConverter.*
import org.json4s.StringInput

import java.util.Date
import scala.xml.{Node, XML}

class CbrRatesProcessor extends CbrRatesAware {
  def toXml(s: String): Node = XML.loadString(s)

  // CBR has no NumCode nor CharCode for (1992-1993):
  //   "Latvijas rublis"  (428 / LVR)
  //   "Lietuvos talonas" (440 / LTT)
  //   "Карбованець"      (804 / UAK)
  val orphanCurrencies: Map[String, (Int, String)] = Map(
    "Латвийский рублис" -> (428, "LVR"),
    "Литовский талон" -> (440, "LTT"),
    "Украинских карбованецев" -> (804, "UAK")
  )

  def parse(observationDate: Date, xml: Node): CbrRates = {
    val (marketName, effectiveDate) = ((xml \ "@name").text, parseDate((xml \ "@Date").text))
    val rates = (xml \ "Valute").map { ccy =>
      val id = (ccy \ "@ID").text.trim
      val name = (ccy \ "Name").text.trim
      val numCode = (ccy \ "NumCode").text.trim match {
        case "" if orphanCurrencies.contains(`name`) => orphanCurrencies(name)._1
        case nc => nc.toInt
      }
      val charCode = (ccy \ "CharCode").text match {
        case "" if orphanCurrencies.contains(`name`) => orphanCurrencies(name)._2
        case cc => cc.trim
      }
      val nominal = (ccy \ "Nominal").text.trim.toInt
      val rate = BigDecimal((ccy \ "Value").text.trim.replace(',', '.'))
      CbrRate(id, numCode, charCode, nominal, name, rate)
    }.toList.sortBy(_.id)

    val key = CbrRatesKey(observationDate, effectiveDate, marketName)
    CbrRates(key, rates)
  }

  def parseDocument(observationDate: Date, report: String): Document = {
    val json = fromJson(xmlStringToJson(report))
    readFrom[Document](StringInput(json))
  }

  def convertRawDataToRates(): Unit = {
    withDb { db =>
      db.withConnection { c =>
        val rawDataDao = RawDataDao(db, c)
        (rawDataDao.firstObservationDate, rawDataDao.lastObservationDate) match {
          case (Some(startDate), Some(endDate)) =>
            val ratesDao = CbrCurrencyRatesDao(db, c)
            ratesDao.createTableIfNeeded()

            var observationDate = startDate
            while (observationDate.getTime <= endDate.getTime) {
              rawDataDao.rawDataOn(observationDate).foreach { report =>
                val xml = toXml(report)
                try {
                  val rates = parse(observationDate, xml)
                  ratesDao.save(rates)
                } catch {
                  case t: Throwable =>
                    println(s"Error on: ${xml}")
                    throw t
                }
              }
              observationDate = nextDate(observationDate)
            }

          case _ =>
        }
      }
    }
  }
}
