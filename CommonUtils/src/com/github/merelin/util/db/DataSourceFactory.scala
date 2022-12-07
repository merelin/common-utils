package com.github.merelin.util.db

import java.util.concurrent.TimeUnit.*

import com.zaxxer.hikari.HikariDataSource

object DataSourceFactory {
  val lock = new Object
  var datasources = Map.empty[DbParams, HikariDataSource]

  def datasource(params: DbParams): HikariDataSource = lock.synchronized {
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
          DbType.fromParams(params).validationQuery.foreach { q => setConnectionTestQuery(q) }

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

  def shutdown(params: DbParams): Unit = lock.synchronized {
    datasources.get(params).foreach {
      ds => DbType.fromParams(params = params) match {
        case H2DbType =>
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

  def shutdownAll(): Unit = lock.synchronized {
    datasources.keys.foreach {
      params =>
        try {
          shutdown(params = params)
        } catch {
          case _: Throwable => println(s"Failed to close ${params}")
        }
    }

    datasources = Map.empty[DbParams, HikariDataSource]
  }
}
