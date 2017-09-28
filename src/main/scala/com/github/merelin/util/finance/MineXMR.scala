package com.github.merelin.util.finance

import java.util.Date
import org.json4s.StringInput
import scala.io.{Codec, Source}
import com.github.merelin.util.json.JsonConverter._
import MineXMR._

case class MineXMRStats(hashes: String, lastShare: String, balance: String, expired: String, thold: String, hashrate: BigDecimal) {
  val pendingBalance = BigDecimal(balance) / denominator
  val totalHashesSubmitted = hashes.toInt
  val lastShareSubmitted = new Date(lastShare.toLong * second)
  val expiredShares = expired.toInt
  val threshold = BigDecimal(thold) / denominator
}

case class MineXMRPayment()

case class MineXMR(stats: MineXMRStats, payments: List[MineXMRPayment])

object MineXMR {
  val second = 1000L
  val denominator = 1000000000000L
  val walletId = "471YaWmbGdah8Y9wGuhY9PNuLG58QwQDsHEx54r8DKPcb2TKiq5cWkDSfodhwaRuWEPZxBbLF16CiAH3a8xkDQXB7tZ9ahE"
  val minexmrUrl = s"http://api.minexmr.com:8080/stats_address?longpoll=false&address=${walletId}"

  def stats = {
    val json = Source.fromURL(minexmrUrl)(Codec("UTF-8")).mkString
    readFrom[MineXMR](StringInput(json))
  }

  def main(args: Array[String]): Unit = {
    System.setProperty("http.proxyHost", "localhost")
    System.setProperty("http.proxyPort", "3128")

    val mineXMR = stats

    println(s"${mineXMR}")
    println(s"pendingBalance: ${mineXMR.stats.pendingBalance} XMR")
    println(s"hashrate: ${mineXMR.stats.hashrate} H/sec")
    println(s"totalHashesSubmitted: ${mineXMR.stats.totalHashesSubmitted}")
    println(s"lastShareSubmitted: ${mineXMR.stats.lastShareSubmitted}")
    println(s"expiredShares: ${mineXMR.stats.expiredShares}")
    println(s"threshold: ${mineXMR.stats.threshold}")
  }
}
