package com.github.merelin.util.math

import scala.math.pow

case class Ratio(numerator: Long, denominator: Long) {
  require(denominator != 0, "denominator should not be zero")

  def gcd: Long = numerator match {
    case 0 => 1
    case _ => Ratio.gcd(numerator.abs, denominator.abs)
  }

  def reduce: Ratio = numerator match {
    case 0 => this
    case _ =>
      val d = Ratio.gcd(numerator.abs, denominator.abs)
      Ratio(
        numerator = (if (denominator < 0) -numerator else numerator) / d,
        denominator = denominator.abs / d
      )
  }

  def doubleValue = numerator.toDouble / denominator.toDouble

  def inverse = Ratio(denominator, numerator)

  def *(l: Long) = Ratio(numerator * l, denominator)

  def *(r: Ratio) = Ratio(numerator * r.numerator, denominator * r.denominator)

  def /(l: Long) = Ratio(numerator, denominator * l)

  def /(r: Ratio) = this * r.inverse

  def ^(power: Int) = {
    def positivePower(r: Ratio, p: Int) = Ratio(pow(r.numerator, p).toLong, pow(r.denominator, p).toLong)

    if (power >= 0) {
      positivePower(this, power)
    } else {
      positivePower(inverse, -power)
    }
  }

  def gcd(r: Ratio) = Ratio(Ratio.gcd(numerator, r.numerator), Ratio.gcd(denominator, r.denominator))

  def numeratorRatio = Ratio(numerator, 1)

  def denominatorRatio = Ratio(denominator, 1)
}

object Ratio {
  private def gcd(a: Long, b: Long): Long = b match {
    case 0 => a
    case _ => gcd(b, a % b)
  }
}
