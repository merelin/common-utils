package com.github.merelin.util.finance.cbr

import java.io.{File, FileWriter}
import java.util.{Calendar, Date}
import com.github.merelin.util.db.*
import com.github.merelin.util.time.Time.*

import java.text.SimpleDateFormat
import scala.io.{Codec, Source}

trait CbrRatesAware {
//  val dbUrl: String = "jdbc:h2:tcp://nas2/finance"
  val dbUrl: String = "jdbc:postgresql://nas1:5432/finance"
  val dbName: String = "Cbr"
  val dbUser: String = "finance_user"
  val dbPassword: String = "f!n@nc3"

  val cbrRatesStartDate = "01.07.1992"

  val dir = "/media/disk3/Nextcloud/Development/common-utils/CommonUtils/resources/Doc"

  def localReportDates: List[Date] = {
    new File(dir).listFiles().filter(_.isDirectory).toList.flatMap(_.listFiles().filter(_.isFile).toList).map(
      _.getName.replace(".xml", "")).map(parseDate).sorted
  }

  def reportFilenameFor(date: Date): String = {
    val year = {
      val cal = Calendar.getInstance(UTC)
      cal.setTime(date)
      cal.get(Calendar.YEAR)
    }
    s"${dir}/${year}/${formatDate(date)}.xml"
  }

  val dateFormat: SimpleDateFormat = {
    val fmt: String = "dd.MM.yyyy"
    val df = new SimpleDateFormat(fmt)
    df.setTimeZone(UTC)
    df
  }

  def parseDate(day: String): Date = synchronized {
    dateFormat.parse(day)
  }

  def formatDate(date: Date): String = synchronized {
    dateFormat.format(date)
  }

  def nextDate(date: Date): Date = {
    val cal = Calendar.getInstance(UTC)
    cal.setTime(date)
    cal.add(Calendar.DATE, 1)
    cal.getTime
  }

  def loadLocalReport(date: Date): String = {
    val file = new File(reportFilenameFor(date))
    Source.fromFile(file).mkString
  }

  def saveLocalReport(date: Date, s: String): Unit = {
    val file = new File(reportFilenameFor(date))
    file.getParentFile.mkdirs()
    val out = new FileWriter(file)
    out.write(s)
    out.flush()
    out.close()
  }

  def withDb[T](fn: Db => T): T = {
    val db = Db(params = DbParams(dbName, dbUrl, dbUser, dbPassword))
    fn(db)
  }
}

class CbrRatesFetcher extends CbrRatesAware {
  val urlBase: String = "http://www.cbr.ru/scripts/XML_daily.asp?date_req="

  def fetch(observationDate: Date): String = {
    Source.fromURL(urlBase + formatDate(observationDate))(Codec("windows-1251")).mkString
  }

  def fetchRange(fromDay: String, toDay: String, pause: Long): Unit = {
    val startDate = parseDate(fromDay)
    val endDate = parseDate(toDay)

    println(s"Fetching range: ${fromDay}-${toDay}")

    var observationDate = startDate
    while (observationDate.getTime <= endDate.getTime) {
      println(s"Fetching date: ${formatDate(observationDate)}")
      val str = fetch(observationDate)
      saveLocalReport(observationDate, str)
      Thread.sleep(pause)
      observationDate = nextDate(observationDate)
    }
  }

  def saveRawData(): Unit = {
    val localDates = localReportDates
    if (localDates.nonEmpty) {
      val startDate = localDates.head
      val endDate = localReportDates.last

      withDb { db =>
        db.withConnection { c =>
          val rawDataDao = RawDataDao(db, c)
          rawDataDao.createTableIfNeeded()
          require(rawDataDao.lastObservationDate.isEmpty, s"Non-empty ${rawDataDao.tableName}!")

          var observationDate = startDate
          while (observationDate.getTime <= endDate.getTime) {
            val str = loadLocalReport(observationDate)
            rawDataDao.save(observationDate, str)
            observationDate = nextDate(observationDate)
          }
        }
      }
    }
  }
}