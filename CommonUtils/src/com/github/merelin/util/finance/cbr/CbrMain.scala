package com.github.merelin.util.finance.cbr

import java.util.Date

object CbrMain {

  // For now is called via:
  //   echo 'CommonUtils.runMain("com.github.merelin.util.finance.cbr.CbrMain")()' | mill -i
  // or
  //   mill CommonUtils.runMain com.github.merelin.util.finance.cbr.CbrMain
  def main(args: Array[String]): Unit = {
    val fetcher = new CbrRatesFetcher
    val lastLocalReportDay = fetcher.localReportDates.last
    val fromDay = fetcher.formatDate(fetcher.nextDate(lastLocalReportDay))
    val toDay = fetcher.formatDate(new Date)

    fetcher.fetchRange(fromDay, toDay, 3000L)
//    fetcher.saveRawData()
//    val processor = new CbrRatesProcessor
//    processor.convertRawDataToRates()
  }
}
