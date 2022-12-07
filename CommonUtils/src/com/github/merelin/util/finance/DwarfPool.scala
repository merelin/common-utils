package com.github.merelin.util.finance

import org.json4s.StringInput
import scala.io.{Codec, Source}
import com.github.merelin.util.json.JsonConverter._

case class DwarfPoolWorkers()

case class DwarfPoolStats(autopayout_from: String, error: Boolean, last_payment_amount: BigDecimal,
                          last_payment_date: Option[String], last_share_date: String, payout_daily: Boolean,
                          payout_request: Boolean, total_hashrate: BigDecimal, total_hashrate_calculated: BigDecimal,
                          wallet: String, wallet_balance: String, workers: DwarfPoolWorkers)

object DwarfPool {
  val walletId = "49Jg7ksRMZiFMbyB2irJ4iJSQtNuWz7pfiwzz8tXdSYHNdSW9HwJpZxKgcL9NctvCSgJYCh4M6LuddP8MdAZWmTr5bbRPwj"
  val dwarfpoolUrl = s"http://dwarfpool.com/xmr/api?email=mail@example.com&wallet=${walletId}"

  def stats = {
    val json = Source.fromURL(dwarfpoolUrl)(Codec("UTF-8")).mkString
    readFrom[DwarfPoolStats](StringInput(json))
  }

  def main(args: Array[String]): Unit = {
    System.setProperty("http.proxyHost", "localhost")
    System.setProperty("http.proxyPort", "3128")

    val dwarfpool = stats

    println(s"${dwarfpool}")
  }
}
