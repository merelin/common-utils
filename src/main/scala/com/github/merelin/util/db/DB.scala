package com.github.merelin.util.db

import java.sql.{Connection, PreparedStatement, ResultSet, Statement}
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
  def withConnection[A](fn: (Connection) => A): A = {
    val connection = datasource.getConnection
    try {
      val result = fn(connection)
      connection.commit()
      result
    } catch {
      case th: Throwable =>
        connection.rollback()
        throw th
    } finally {
      if (connection != null)
        connection.close()
    }
  }

  def withStatement[A](st: Statement)(fn: (Statement) => A): A = try { fn(st) } finally { if (st != null) st.close() }

  def withResultSet[A](rs: ResultSet)(fn: (ResultSet) => A): A = try { fn(rs) } finally { if (rs != null) rs.close() }

  def tableExists(connection: Connection, table: String): Boolean =
    withResultSet[Boolean](connection.getMetaData.getTables(null, null, table.toUpperCase, null)) { rs => rs.next() }

  def createTable(connection: Connection, sql: String): Unit =
    withStatement[Boolean](connection.prepareStatement(sql)) { st => st.asInstanceOf[PreparedStatement].execute() }
}

object DB {
  def apply(params: DBParams): DB =
    new DB(params = params, datasource = DataSourceFactory.datasource(params), dbType = DBType.fromParams(params))
}
