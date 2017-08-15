package com.github.merelin.util.region

import java.util.{Locale => JLocale}
import com.ibm.icu.util.ULocale

case class Locale(country: String, language: String) {
  require(country.nonEmpty, "country should be specified")
  require(language.nonEmpty, "language should be specified")
}

object Locale {
  def apply(jLocale: JLocale): Locale = new Locale(country = jLocale.getCountry, language = jLocale.getLanguage)
  def apply(uLocale: ULocale): Locale = new Locale(country = uLocale.getCountry, language = uLocale.getLanguage)
}
