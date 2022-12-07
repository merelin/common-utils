package com.github.merelin.util.db

import java.sql.{Connection, PreparedStatement, ResultSet, Statement}
import javax.sql.DataSource

sealed trait DbType {
  def validationQuery: Option[String]
}

case object OracleDbType extends DbType {
  override val validationQuery = Some("select 1 from dual")
}

case object MsSqlDbType extends DbType {
  override val validationQuery = Some("select 1")
}

case object PostgreSqlDbType extends DbType {
  override val validationQuery = Some("select 1")
}

case object H2DbType extends DbType {
  override val validationQuery = Some("select 1")
}

case object UnknownDbType extends DbType {
  override val validationQuery = None
}

object DbType {
  def fromUrl(url: String): DbType = url.split(":").toList match {
    case "jdbc" :: "oracle" :: _ => OracleDbType
    case "jdbc" :: "jtds" :: "sqlserver" :: _ => MsSqlDbType
    case "jdbc" :: "postgresql" :: _ => PostgreSqlDbType
    case "jdbc" :: "h2" :: _ => H2DbType
    case _ => UnknownDbType
  }

  def fromParams(params: DbParams): DbType = fromUrl(params.url)
}

// name may not contain any of the characters ,, =, :, ', *, or ? ...
case class DbParams(name: String, url: String, username: String, password: String) {
  val objectName = name.replaceAll("""[,=:'\*\?\"]""", "_")
}

case class Db(params: DbParams, datasource: DataSource, dbType: DbType) {
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

  def withStatement[A](st: PreparedStatement)(fn: (PreparedStatement) => A): A =
    try { fn(st) } finally { if (st != null) st.close() }

  def withResultSet[A](rs: ResultSet)(fn: (ResultSet) => A): A =
    try { fn(rs) } finally { if (rs != null) rs.close() }

  def tableExists(connection: Connection, table: String): Boolean = {
    // NB: PostgreSQL is unable to find a table when it's name is in UPPERCASE. Hence table.toLowerCase.
    val tableName = table.toLowerCase
    withResultSet[Boolean](connection.getMetaData.getTables(null, null, tableName, null)) { rs => rs.next() }
  }

  def createTable(connection: Connection, sql: String): Unit =
    withStatement[Boolean](connection.prepareStatement(sql)) { st => st.execute() }
}

object Db {
  def apply(params: DbParams): Db = {
    Db(params = params, datasource = DataSourceFactory.datasource(params), dbType = DbType.fromParams(params))
  }
}
