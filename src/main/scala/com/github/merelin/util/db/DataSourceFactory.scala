package com.github.merelin.util.db

import java.sql.DriverManager
import java.util.concurrent.TimeUnit._

import com.zaxxer.hikari.HikariDataSource

object DataSourceFactory {
  List(
//    new oracle.jdbc.driver.OracleDriver(),
//    new net.sourceforge.jtds.jdbc.Driver(),
    new org.h2.Driver()
  ).foreach(DriverManager.registerDriver)

  val lock = new Object
  var datasources = Map.empty[DBParams, HikariDataSource]

  def datasource(params: DBParams): HikariDataSource = lock.synchronized {
    datasources.getOrElse(
      params,
      {
        val ds = new HikariDataSource {
          setPoolName(params.objectName)
          setAutoCommit(false)
          setJdbcUrl(params.url)
          setUsername(params.username)
          setPassword(params.password)

          // work-around to a HikariCP bug:
          // if no connection test query is set, Hikari calls JDBC4 isValid method on a newly created connection,
          // even if it is not implemented, which is so in jTDS
          DBType.fromParams(params).validationQuery.foreach { q => setConnectionTestQuery(q) }

          setValidationTimeout(5000)
          setIdleTimeout(300000)
          setConnectionTimeout(60000)
          setLeakDetectionThreshold(MINUTES.toMillis(10))
          setRegisterMbeans(true)
          setMinimumIdle(2)
          setMaximumPoolSize(40)
        }

        datasources += params -> ds

        ds
      }
    )
  }

  def shutdown(params: DBParams): Unit = lock.synchronized {
    datasources.get(params).foreach {
      ds => DBType.fromParams(params = params) match {
        case H2DBType =>
          // H2 doesn't close without an explicit SHUTDOWN
          ds.getConnection.prepareCall("SHUTDOWN").executeUpdate()
          try {
            ds.close()
          } catch {
            case th: Throwable =>
              // harmless, SHUTDOWN often closes the connection
            println("explicit close failed after SHUTDOWN statement")
          }

        case _ =>
          ds.close()
      }

      datasources -= params
    }
  }

  def shutdownAll() = lock.synchronized {
    datasources.keys.foreach {
      params =>
        try {
          shutdown(params = params)
        } catch {
          case th: Throwable => println(s"Failed to close ${params}")
        }
    }

    datasources = Map.empty[DBParams, HikariDataSource]
  }
}
