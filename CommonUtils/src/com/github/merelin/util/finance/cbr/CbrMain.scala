package com.github.merelin.util.finance.cbr

object CbrMain {

  // For now is called via:
  //   echo 'CommonUtils.runMain("com.github.merelin.util.finance.cbr.CbrMain")()' | mill -i
  def main(args: Array[String]): Unit = {
    val fetcher = new CbrRatesFetcher
    fetcher.fetchRange("03.12.2022", "10.12.2022", 3000L)
//    fetcher.saveRawData()
//    val processor = new CbrRatesProcessor
//    processor.convertRawDataToRates()
  }
}
