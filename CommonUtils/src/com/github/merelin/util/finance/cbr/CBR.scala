package com.github.merelin.util.finance.cbr

import com.github.merelin.util.db.{Db, DbParams}
import com.github.merelin.util.finance.*
import com.github.merelin.util.json.JsonConverter.*
import com.github.merelin.util.region.Currency
import com.github.merelin.util.time.Time.*
import org.json4s.StringInput

import java.sql.{Connection, PreparedStatement, Date as SQLDate}
import java.util.{Calendar, Date}
import scala.io.{Codec, Source}

//case class Document(ValCurs: ValCurs) {
//  def toRates(observationDate: Date) = ValCurs.Valute.flatMap {
//    v =>
//      Currency.get(v.CharCode).map {
//        currency =>
//          Rate(
//            id = None,
//            observationDate = observationDate,
//            effectiveDate = parseUTC(CBR.format, ValCurs.Date),
//            numCode = currency.numCode,
//            charCode = currency.charCode,
//            nominal = v.Nominal.toInt,
//            value = BigDecimal(v.Value.replace(",", "."))
//          )
//      }.toList
//  }
//}
//
//case class Rate(id: Option[Long], observationDate: Date, effectiveDate: Date, numCode: String, charCode: String,
//                nominal: Int, value: BigDecimal)

object CBR {
//  val dbUrl: String = "jdbc:h2:tcp://nas2/finance"
  val dbUrl: String = "jdbc:h2:file:/tmp/finance;IFEXISTS=TRUE;MVCC=TRUE;AUTO_SERVER=TRUE"
  val urlBase: String = "http://www.cbr.ru/scripts/XML_daily.asp?date_req="

  val format: String = "dd.MM.yyyy"

  def url(date: Date): String = urlBase + formatUTC(format, date)

  def httpGet(url: String): String = Source.fromURL(url)(Codec("windows-1251")).mkString

//  def fetchRatesOn(observationDate: Date): Document = {
//    val xmlString = httpGet(url(observationDate))
//    val json = fromJson(xmlStringToJson(xmlString))
//
//    readFrom[Document](StringInput(json))
//  }

  def main(args: Array[String]): Unit = {
//    Map("http.proxyHost" -> "localhost", "http.proxyPort" -> "3128").foreach((System.setProperty _).tupled)
//
//    val observationDate = parseUTC(format, "23.08.2017")
//    val document = fetchRatesOn(observationDate)
//    val rates = document.toRates(observationDate)
//
//    val db = Db(params = DbParams(name = "expenses", url = dbUrl, username = "", password = ""))
//    db.withConnection {
//      conn =>
//        CBRDAO.createCBRRatesTableIfNeeded(db, conn)
//        val rowsModified = CBRDAO.save(db, conn, rates)
//
//        println(s"${rowsModified} row(s) inserted")
//
//        val loaded = CBRDAO.load(db, conn, observationDate).map(r => r.copy(id = None)).distinct
//        if (loaded != rates) {
//          loaded.zipAll(rates, "", "").map {
//            case (l, r) => if (l != r) println(s"l = ${l} != r: ${r}")
//          }
//        }
//    }
  }
}
