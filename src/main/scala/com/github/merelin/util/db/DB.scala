package com.github.merelin.util.db

import java.sql.{Connection, ResultSet, Statement}
import javax.sql.DataSource

sealed trait DBType {
  def validationQuery: Option[String]
}

case object OracleDBType extends DBType {
  override val validationQuery = Some("select 1 from dual")
}

case object MsSqlDBType extends DBType {
  override val validationQuery = Some("select 1")
}

case object H2DBType extends DBType {
  override val validationQuery = Some("select 1")
}

case object UnknownDBType extends DBType {
  override val validationQuery = None
}

object DBType {
  def fromUrl(url: String): DBType = url.split(":").toList match {
    case "jdbc" :: "oracle" :: rest => OracleDBType
    case "jdbc" :: "jtds" :: "sqlserver" :: rest => MsSqlDBType
    case "jdbc" :: "h2" :: rest => H2DBType
    case _ => UnknownDBType
  }

  def fromParams(params: DBParams): DBType = fromUrl(params.url)
}

// name may not contain any of the characters ,, =, :, ', *, or ? ...
case class DBParams(name: String, url: String, username: String, password: String) {
  val objectName = name.replaceAll("""[,=:'\*\?\"]""", "_")
}

case class DB(params: DBParams, datasource: DataSource, dbType: DBType) {
  def withConnection[A](fn: (Connection) => A): A = fn(datasource.getConnection)
  def withStatement[A](st: Statement)(fn: (Statement) => A): A = try { fn(st) } finally { st.close() }
  def withResultSet[A](rs: ResultSet)(fn: (ResultSet) => A): A = try { fn(rs) } finally { rs.close() }
}

object DB {
  def apply(params: DBParams): DB = new DB(
    params = params, datasource = DataSourceFactory.datasource(params), dbType = DBType.fromParams(params)
  )
}