package com.github.merelin.util.region

object CurrencyTest {
  def main(args: Array[String]): Unit = {
    import Currency._

    require((currencies -- obsoleteCurrencies -- pseudoCurrencies) == currencies)

    require(
      jvmCurrencies.filterNot(c => currencies.exists(cc => c.getCurrencyCode == cc.charCode))
        .filterNot(c => obsoleteCurrencies.exists(cc => c.getCurrencyCode == cc.charCode))
        .filterNot(c => pseudoCurrencies.exists(cc => c.getCurrencyCode == cc.charCode)).isEmpty
    )

    require(
      icuCurrencies.filterNot(c => currencies.exists(cc => c.getCurrencyCode == cc.charCode))
        .filterNot(c => obsoleteCurrencies.exists(cc => c.getCurrencyCode == cc.charCode))
        .filterNot(c => pseudoCurrencies.exists(cc => c.getCurrencyCode == cc.charCode)).isEmpty
    )
  }
}
