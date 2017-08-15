package com.github.merelin.util.region

import java.util.{Locale => JLocale}

import com.ibm.icu.util.{ULocale, Region => URegion}
import com.ibm.icu.util.Region.RegionType._

case class Region(locale: Locale, currency: Currency)

object Region {
  def apply(jLocale: JLocale): Region =
    new Region(locale = Locale(jLocale = jLocale), currency = Currency(jLocale = jLocale))

  def apply(uLocale: ULocale): Region =
    new Region(locale = Locale(uLocale = uLocale), currency = Currency(uLocale = uLocale))

  def isCountry(country: String): Boolean = {
    val uRegion = URegion.getInstance(country)
    uRegion.getType match {
      case TERRITORY => true
      case CONTINENT | DEPRECATED | GROUPING | SUBCONTINENT | UNKNOWN | WORLD => false
    }
  }

  // Oracle JVM available locales
  val jvmRegions: Set[Region] = JLocale.getAvailableLocales.collect {
    case l if l.getLanguage.nonEmpty && l.getCountry.nonEmpty => Region(jLocale = l)
  }.toSet

  // IBM ICU available locales
  val icuRegions: Set[Region] = ULocale.getAvailableLocales.collect {
    case l if l.getLanguage.nonEmpty && l.getCountry.nonEmpty && isCountry(l.getCountry) => Region(uLocale = l)
  }.toSet

  // Missing from Oracle JVM and IBM ICU available locales
  val missingRegions = Set(
    "tg" -> "TJ", // Tajikistan
    "tk" -> "TM"  // Turkmenistan
  ).map { case (l, c) => Region(uLocale = ULocale.forLocale(new JLocale(l, c))) }

  // Obsolete locales present in Oracle JVM or IBM ICU
  val obsoleteLocales = Set(
    "sr" -> "CS"  // Serbia and Montenegro
  )

  def isObsolete(region: Region): Boolean = obsoleteLocales.contains(region.locale.language -> region.locale.country)

  // Regions for all available countries
  val regions: Set[Region] = (jvmRegions ++ icuRegions ++ missingRegions).filterNot(isObsolete)
}
