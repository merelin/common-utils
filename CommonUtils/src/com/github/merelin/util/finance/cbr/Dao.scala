package com.github.merelin.util.finance.cbr

import java.sql.{Date => SQLDate}
import java.sql._
import java.util.Date
import com.github.merelin.util.db._
import com.github.merelin.util.time.Time._

import java.util.Calendar

trait Dao {
  def tableName: String

  def tableCreationQuery: String

  def db: Db
  def connection: Connection

  def createTableIfNeeded(): Unit = {
    if (!db.tableExists(connection, tableName)) {
      db.createTable(connection, tableCreationQuery)
    }
  }
}

case class Valute(ID: String, NumCode: String, CharCode: String, Name: String, Nominal: String, Value: String)
case class ValCurs(name: String, Date: String, Valute: List[Valute])
case class Document(ValCurs: ValCurs)

case class CbrRate(id: String, numCode: Int, charCode: String, nominal: Int, name: String, rate: BigDecimal)
case class CbrRatesKey(observationDate: Date, effectiveDate: Date, marketName: String)
case class CbrRates(key: CbrRatesKey, rates: List[CbrRate])

case class RawDataDao(db: Db, connection: Connection) extends Dao {
  val tableName: String = "CbrRawData"
  val tableCreationQuery: String = s"create table ${tableName} (ObservationDate date, RawData varchar)"

  def firstObservationDate: Option[Date] = {
    db.withConnection { c =>
      db.withStatement(c.prepareStatement(s"select min(ObservationDate) from ${tableName}")) { st =>
        db.withResultSet(st.executeQuery()) { rs =>
          if (rs.next())
            Option(rs.getDate(1, Calendar.getInstance(UTC))).map(d => new Date(d.getTime))
          else
            None
        }
      }
    }
  }

  def lastObservationDate: Option[Date] = {
    db.withConnection { c =>
      db.withStatement(c.prepareStatement(s"select max(ObservationDate) from ${tableName}")) { st =>
        db.withResultSet(st.executeQuery()) { rs =>
          if (rs.next())
            Option(rs.getDate(1, Calendar.getInstance(UTC))).map(d => new Date(d.getTime))
          else
            None
        }
      }
    }
  }

  def rawDataOn(observationDate: Date): Option[String] = {
    db.withConnection { c =>
      db.withStatement(c.prepareStatement(s"select RawData from ${tableName} where ObservationDate = ?")) { st =>
        db.withResultSet {
          st.setDate(1, new SQLDate(observationDate.getTime), Calendar.getInstance(UTC))
          st.executeQuery()
        } { rs =>
          if (rs.next()) Some(rs.getString(1)) else None
        }
      }
    }
  }

  def save(observationDate: Date, rawData: String): Unit = {
    db.withConnection { c =>
      db.withStatement(c.prepareStatement(s"insert into ${tableName} (ObservationDate, RawData) values (?, ?)")) { st =>
        st.setDate(1, new SQLDate(observationDate.getTime), Calendar.getInstance(UTC))
        st.setString(2, rawData)
        st.execute()
      }
    }
  }
}

case class CbrCurrencyRatesDao(db: Db, connection: Connection) extends Dao {
  val tableName: String = "CbrCurrencyRates"
  val tableCreationQuery: String =
    s"""create table ${tableName} (
       |  Id varchar,
       |  NumCode int,
       |  CharCode varchar,
       |  Nominal int,
       |  Name varchar,
       |  Rate decimal,
       |  ObservationDate date,
       |  EffectiveDate date,
       |  MarketName varchar
       |)""".stripMargin

  def ratesOn(observationDate: Date): List[CbrRates] = {
    db.withConnection { c =>
      db.withStatement(c.prepareStatement(s"select * from ${tableName} where ObservationDate = ? order by Id")) { st =>
        db.withResultSet {
          st.setDate(1, new SQLDate(observationDate.getTime), Calendar.getInstance(UTC))
          st.executeQuery()
        } { rs =>
          var result: List[(CbrRatesKey, CbrRate)] = List.empty[(CbrRatesKey, CbrRate)]

          while (rs.next()) {
            val id = rs.getString(1)
            val numCode = rs.getInt(2)
            val charCode = rs.getString(3)
            val nominal = rs.getInt(4)
            val name = rs.getString(5)
            val rate = rs.getBigDecimal(6)
            val observationDate = new Date(rs.getDate(7, Calendar.getInstance(UTC)).getTime)
            val effectiveDate = new Date(rs.getDate(8, Calendar.getInstance(UTC)).getTime)
            val marketName = rs.getString(9)

            val cbrRate = CbrRate(id, numCode, charCode, nominal, name, rate)
            val cbrRatesKey = CbrRatesKey(observationDate, effectiveDate, marketName)
            result :+= cbrRatesKey -> cbrRate
          }

          result.groupBy { case (key, _) => key }.map { case (key, pair) =>
            val rates = pair.map { case (_, value) => value }
            CbrRates(key, rates)
          }.toList.sortBy(_.key.observationDate)
        }
      }
    }
  }

  def save(rates: CbrRates): Unit = {
    db.withConnection { c =>
      db.withStatement(c.prepareStatement(
        s"""insert into ${tableName}
           |  (Id, NumCode, CharCode, Nominal, Name, Rate, ObservationDate, EffectiveDate, MarketName)
           |  values (?, ?, ?, ?, ?, ?, ?, ?, ?)
        """.stripMargin
      )) { st =>
        val key = rates.key
        for (rate <- rates.rates) {
          st.setString(1, rate.id)
          st.setInt(2, rate.numCode)
          st.setString(3, rate.charCode)
          st.setInt(4, rate.nominal)
          st.setString(5, rate.name)
          st.setBigDecimal(6, rate.rate.bigDecimal)
          st.setDate(7, new SQLDate(key.observationDate.getTime), Calendar.getInstance(UTC))
          st.setDate(8, new SQLDate(key.effectiveDate.getTime), Calendar.getInstance(UTC))
          st.setString(9, key.marketName)
          st.addBatch()
        }
        st.executeBatch()
      }
    }
  }
}
