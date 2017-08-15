package com.github.merelin.util.time

import java.text.SimpleDateFormat
import java.util.{Date, TimeZone}

object Time {
  val UTC = TimeZone.getTimeZone("UTC")

  def format(fmt: String, date: Date, tz: TimeZone): String = {
    val df = new SimpleDateFormat(fmt)
    df.setTimeZone(UTC)

    df.format(date)
  }

  def formatUTC(fmt: String, date: Date): String = format(fmt, date, UTC)

  def parse(fmt: String, s: String, tz: TimeZone): Date = {
    val df = new SimpleDateFormat(fmt)
    df.setTimeZone(tz)

    df.parse(s)
  }

  def parseUTC(fmt: String, s: String): Date = parse(fmt, s, UTC)
}
