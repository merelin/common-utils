package com.github.merelin.util.region

import java.util.{Currency => JCurrency, Locale => JLocale}

import com.ibm.icu.util.{ULocale, Currency => UCurrency}

import scala.collection.JavaConverters._
import scala.util.Try

case class Names(enUS: String, jvmOpt: Option[String], icuOpt: Option[String])

case class Symbols(enUS: String, jvmOpt: Option[String], icuOpt: Option[String]) {
  // ¤ or \u00A4 is a general currency symbol
  val generalCurrencySymbol = "¤"
}

case class Currency(charCode: String, number: Int, decimalPlaces: Int, symbols: Symbols, names: Names) {
  val numCode: String = f"${number}%03d"
}

object Currency {
  import Region._

  // Actual currencies for all available countries
  lazy val currencies: Set[Currency] = regions.map(_.currency)

  val obsoleteCurrencyCodes = Set(
    "ARA", // Argentine Austral
    "ARP", // Argentine Peso (1983–1985)
    "CSD", // Serbian Dinar (2002–2006)
    "HRD", // Croatian Dinar
    "PEI", // Peruvian Inti
    "PES", // Peruvian Sol (1863–1965)
    "YUM"  // Yugoslavian New Dinar (1994–2002)
  )

  def isObsoleteCurrency(jCurrency: JCurrency): Boolean =
    obsoleteCurrencyCodes.contains(jCurrency.getCurrencyCode) || currencies.forall(_.number != jCurrency.getNumericCode)

  def isObsoleteCurrency(uCurrency: UCurrency): Boolean =
    obsoleteCurrencyCodes.contains(uCurrency.getCurrencyCode) || currencies.forall(_.number != uCurrency.getNumericCode)

  // Oracle JVM available currencies
  lazy val jvmCurrencies: Set[JCurrency] = JCurrency.getAvailableCurrencies.asScala.toSet

  // IBM ICU available currencies
  lazy val icuCurrencies: Set[UCurrency] = UCurrency.getAvailableCurrencies.asScala.toSet

  // Obsolete currencies
  lazy val obsoleteCurrencies: Set[Currency] =
    jvmCurrencies.collect {
      case c if isObsoleteCurrency(c) =>
        Currency(
          charCode = c.getCurrencyCode,
          number = c.getNumericCode,
          decimalPlaces = c.getDefaultFractionDigits,
          symbols = Symbols(
            enUS = c.getSymbol(JLocale.US),
            jvmOpt = Some(c.getSymbol(JLocale.US)),
            icuOpt = Some(c.getSymbol(JLocale.US))
          ),
          names = Names(
            enUS = c.getDisplayName(JLocale.US),
            jvmOpt = Some(c.getDisplayName(JLocale.US)),
            icuOpt = Some(c.getDisplayName(JLocale.US))
          )
        )
    } ++ icuCurrencies.collect {
      case c if isObsoleteCurrency(c) =>
        Currency(
          charCode = c.getCurrencyCode,
          number = c.getNumericCode,
          decimalPlaces = c.getDefaultFractionDigits,
          symbols = Symbols(
            enUS = c.getSymbol(JLocale.US),
            jvmOpt = Some(c.getSymbol(JLocale.US)),
            icuOpt = Some(c.getSymbol(JLocale.US))
          ),
          names = Names(
            enUS = c.getDisplayName(JLocale.US),
            jvmOpt = Some(c.getDisplayName(JLocale.US)),
            icuOpt = Some(c.getDisplayName(JLocale.US))
          )
        )
    }

  lazy val pseudoCurrencies: Set[Currency] = obsoleteCurrencies.collect { case c if c.decimalPlaces < 0 => c }

  def apply(jLocale: JLocale): Currency = {
    val jCurrency = JCurrency.getInstance(jLocale)
    val uLocale = ULocale.forLocale(jLocale)

    new Currency(
      charCode = jCurrency.getCurrencyCode,
      number = jCurrency.getNumericCode,
      decimalPlaces = jCurrency.getDefaultFractionDigits,
      symbols = Symbols(
        enUS = jCurrency.getSymbol(JLocale.US),
        jvmOpt = Try { jCurrency.getSymbol(jLocale) }.toOption,
        icuOpt = Try { UCurrency.getInstance(uLocale).getSymbol(uLocale) }.toOption
      ),
      names = Names(
        enUS = jCurrency.getDisplayName(JLocale.US),
        jvmOpt = Try { jCurrency.getDisplayName(jLocale) }.toOption,
        icuOpt = Try { UCurrency.getInstance(uLocale).getName(uLocale, UCurrency.LONG_NAME, null) }.toOption
      )
    )
  }

  def apply(uLocale: ULocale): Currency = {
    val uCurrency = UCurrency.getInstance(uLocale)
    val jLocale = uLocale.toLocale

    new Currency(
      charCode = uCurrency.getCurrencyCode,
      number = uCurrency.getNumericCode,
      decimalPlaces = uCurrency.getDefaultFractionDigits,
      symbols = Symbols(
        enUS = uCurrency.getSymbol(ULocale.forLocale(JLocale.US)),
        jvmOpt = Try { JCurrency.getInstance(jLocale).getSymbol(jLocale) }.toOption,
        icuOpt = Try { uCurrency.getSymbol(uLocale) }.toOption
      ),
      names = Names(
        enUS = uCurrency.getName(ULocale.forLocale(JLocale.US), UCurrency.LONG_NAME, null),
        jvmOpt = Try { JCurrency.getInstance(jLocale).getDisplayName(jLocale) }.toOption,
        icuOpt = Try { uCurrency.getName(uLocale, UCurrency.LONG_NAME, null) }.toOption
      )
    )
  }

  def get(number: Int): Option[Currency] = currencies.find(_.number == number)
  def get(charCode: String): Option[Currency] = currencies.find(_.charCode == charCode)
  def getObsolete(number: Int): Option[Currency] = obsoleteCurrencies.find(_.number == number)
  def getObsolete(charCode: String): Option[Currency] = obsoleteCurrencies.find(_.charCode == charCode)
  def getPseudo(number: Int): Option[Currency] = pseudoCurrencies.find(_.number == number)
  def getPseudo(charCode: String): Option[Currency] = pseudoCurrencies.find(_.charCode == charCode)
}
