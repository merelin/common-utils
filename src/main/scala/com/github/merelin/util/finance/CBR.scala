package com.github.merelin.util.finance

import java.sql.{Connection, PreparedStatement, Date => SQLDate}
import java.util.{Calendar, Date}

import com.github.merelin.util.db.{DB, DBParams}
import com.github.merelin.util.json.JsonConverter._
import com.github.merelin.util.region.Currency
import com.github.merelin.util.time.Time._
import org.json4s.StringInput

import scala.io.{Codec, Source}

case class Valute(ID: String, NumCode: String, CharCode: String, Name: String, Nominal: String, Value: String)
case class ValCurs(name: String, Date: String, Valute: List[Valute])

case class Document(ValCurs: ValCurs) {
  def toRates(observationDate: Date) = ValCurs.Valute.flatMap {
    v =>
      Currency.get(v.CharCode).map {
        currency =>
          Rate(
            id = None,
            observationDate = observationDate,
            effectiveDate = parseUTC(CBR.format, ValCurs.Date),
            numCode = currency.numCode,
            charCode = currency.charCode,
            nominal = v.Nominal.toInt,
            value = BigDecimal(v.Value.replace(",", "."))
          )
      }.toList
  }
}

case class Rate(id: Option[Long], observationDate: Date, effectiveDate: Date, numCode: String, charCode: String,
                nominal: Int, value: BigDecimal)

object CBRDAO {
  val tableName: String = "CBRRates"

  def createCBRRatesTableIfNeeded(db: DB, connection: Connection): Unit = {
    if (! db.tableExists(connection, tableName)) {
      db.createTable(
        connection,
        s"""create table ${tableName} (
           |  Id BigInt auto_increment primary key,
           |  ObservationDate Date,
           |  EffectiveDate Date,
           |  NumCode Varchar2(16),
           |  CharCode varchar2(16),
           |  Nominal int,
           |  Value decimal
           |)""".stripMargin
      )
    }
  }

  def load(db: DB, connection: Connection, observationDate: Date): List[Rate] = {
    db.withConnection {
      c => db.withStatement(c.prepareStatement(s"select * from ${tableName} where ObservationDate = ?")) {
        db.withStatement(_) {
          st => db.withResultSet {
            val ps = st.asInstanceOf[PreparedStatement]
            ps.setDate(1, new SQLDate(observationDate.getTime), Calendar.getInstance(UTC))
            ps.executeQuery()
          } {
            rs =>
              var result: List[Rate] = Nil
              while (rs.next()) {
                result :+= Rate(
                  id = Some(rs.getLong("Id")),
                  observationDate = new Date(rs.getDate("ObservationDate", Calendar.getInstance(UTC)).getTime),
                  effectiveDate = new Date(rs.getDate("EffectiveDate", Calendar.getInstance(UTC)).getTime),
                  numCode = rs.getString("NumCode"),
                  charCode = rs.getString("CharCode"),
                  nominal = rs.getInt("Nominal"),
                  value = rs.getBigDecimal("Value")
                )
              }

              result
          }
        }
      }
    }
  }

  def save(db: DB, connection: Connection, rates: List[Rate]): Unit = {
    db.withConnection {
      c => db.withStatement(c.prepareStatement(
//        s"""merge into ${tableName}
//           |  (Id, NumCode, CharCode, Nominal, Name, Rate, ObservationDate, EffectiveDate, InfoName)
//           |  key (NumCode)
//           |  values (?, ?, ?, ?, ?, ?, ?, ?, ?)
//        """.stripMargin
        s"""insert into ${tableName}
           |  (ObservationDate, EffectiveDate, NumCode, CharCode, Nominal, Value)
           |  values (?, ?, ?, ?, ?, ?)
        """.stripMargin
      )) {
        db.withStatement(_) {
          st =>
            val ps = st.asInstanceOf[PreparedStatement]

            for (rate <- rates) {
              ps.setDate(1, new SQLDate(rate.observationDate.getTime), Calendar.getInstance(UTC))
              ps.setDate(2, new SQLDate(rate.effectiveDate.getTime), Calendar.getInstance(UTC))
              ps.setString(3, rate.numCode)
              ps.setString(4, rate.charCode)
              ps.setInt(5, rate.nominal)
              ps.setBigDecimal(6, rate.value.bigDecimal)

              ps.addBatch()
            }

            ps.executeBatch()
        }
      }
    }
  }
}

object CBR {
//  val dbUrl: String = "jdbc:h2:tcp://nas2/finance"
  val dbUrl: String = "jdbc:h2:file:/tmp/finance;IFEXISTS=TRUE;MVCC=TRUE;AUTO_SERVER=TRUE"
  val urlBase: String = "http://www.cbr.ru/scripts/XML_daily.asp?date_req="

  val format: String = "dd.MM.yyyy"

  def url(date: Date): String = urlBase + formatUTC(format, date)

  def httpGet(url: String): String = Source.fromURL(url)(Codec("windows-1251")).mkString

  def fetchRatesOn(observationDate: Date): Document = {
    val xmlString = httpGet(url(observationDate))
    val json = fromJson(xmlStringToJson(xmlString))

    readFrom[Document](StringInput(json))
  }

  def main(args: Array[String]) {
    Map("http.proxyHost" -> "localhost", "http.proxyPort" -> "3128").foreach((System.setProperty _).tupled)

    val observationDate = parseUTC(format, "23.08.2017")
    val document = fetchRatesOn(observationDate)
    val rates = document.toRates(observationDate)

    val db = DB(params = DBParams(name = "expenses", url = dbUrl, username = "", password = ""))
    db.withConnection {
      conn =>
        CBRDAO.createCBRRatesTableIfNeeded(db, conn)
        val rowsModified = CBRDAO.save(db, conn, rates)

        println(s"${rowsModified} row(s) inserted")

        val loaded = CBRDAO.load(db, conn, observationDate).map(r => r.copy(id = None)).distinct
        if (loaded != rates) {
          loaded.zipAll(rates, "", "").map {
            case (l, r) => if (l != r) println(s"l = ${l} != r: ${r}")
          }
        }
    }
  }
}
