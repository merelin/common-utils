package com.github.merelin.util.db

import java.sql.PreparedStatement

object DBTest {
  def main(args: Array[String]): Unit = {
    val db = DB(
      params = DBParams(name = "h2-KRTAdapter-dev", url = "jdbc:h2:/tmp/h2-KRTAdapter-dev", username = "", password = "")
    )
    val actual = db.withConnection[List[Int]] {
      con =>
        var list = List.empty[Int]

        db.withStatement(con.prepareStatement("select 1")) {
          st => db.withResultSet(st.asInstanceOf[PreparedStatement].executeQuery()) {
            rs => while (rs.next) { list :+= rs.getInt(1) }
          }
        }

        list
    }

    require(actual == List(1))

    DataSourceFactory.shutdown(params = db.params)
  }
}
