package com.github.merelin.util.region

object RegionTest {
  def main(args: Array[String]): Unit = {
    import Region._

    require(regions.filterNot(isObsolete) == regions)
  }
}
