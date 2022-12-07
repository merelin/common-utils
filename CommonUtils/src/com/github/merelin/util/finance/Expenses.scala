package com.github.merelin.util.finance

import java.io.File
import java.math.BigDecimal
import java.sql.{Connection, PreparedStatement}
import java.util.{Calendar, Date}

import com.github.merelin.util.db.{Db, DbParams}
import com.github.merelin.util.time.Time._

import scala.io.Source

case class Expense(id: Option[Long], day: Date, amount: BigDecimal, description: String)

object TextParser {
  def parseExpenses(txt: File): List[Expense] = {
    var expenses: List[Expense] = Nil

    Source.fromFile(txt).getLines().toList.filterNot(_.isEmpty).map {
      line =>
        val values = line.split("\t", -1).toList
        require(values.size == 3)

        val List(dayStr, amountStr, description) = values

        expenses :+= Expense(
          id = None,
          day = parseUTC("dd.MM.yyyy", dayStr.trim),
          amount = new BigDecimal(amountStr.trim),
          description = description
        )
    }

    expenses
  }
}

object ExpensesDAO {
  val tableName: String = "Expenses"

  def createExpensesTableIfNeeded(db: Db, connection: Connection): Unit = {
    if (! db.tableExists(connection, tableName)) {
      db.createTable(
        connection,
        """create table Expenses (
          |  Id BigInt auto_increment primary key,
          |  Day Date,
          |  Amount Decimal,
          |  Description Varchar(1024)
          |)
        """.stripMargin
      )
    }
  }

  def loadExpenses(db: Db, connection: Connection): List[Expense] = {
    db.withConnection {
      c => db.withStatement(c.prepareStatement(s"select * from ${tableName}")) {
        db.withStatement(_) {
          st => db.withResultSet(st.asInstanceOf[PreparedStatement].executeQuery()) {
            rs =>
              var expenses: List[Expense] = Nil
              while (rs.next) {
                expenses :+= Expense(
                  id = Some(rs.getLong("Id")),
                  day = new Date(rs.getDate("Day", Calendar.getInstance(UTC)).getTime),
                  amount = rs.getBigDecimal("Amount"),
                  description = rs.getString("Description")
                )
              }

              expenses
          }
        }
      }
    }
  }

  def saveExpenses(db: Db, connection: Connection, expenses: List[Expense]): Int = {
    db.withConnection {
      c =>
        db.withStatement(c.prepareStatement(
//          "merge into Expenses (Day, Amount, Description) key (Day, Amount, Description) values (?, ?, ?)"
          s"insert into ${tableName} (Day, Amount, Description) values (?, ?, ?)"
        )) {
          db.withStatement(_) {
            st =>
              val ps = st.asInstanceOf[PreparedStatement]

              for (expense <- expenses) {
                ps.setDate(1, new java.sql.Date(expense.day.getTime), Calendar.getInstance(UTC))
                ps.setBigDecimal(2, expense.amount)
                ps.setString(3, expense.description)

                ps.addBatch()
              }

              ps.executeBatch()
          }
        }
    }.sum
  }
}

object Expenses {
//  val dbUrl = "jdbc:h2:tcp://nas2/finance"
//  val file = new File("/home/mantonyan/Desktop/расходы")
  val dbUrl = "jdbc:h2:file:/tmp/finance;IFEXISTS=TRUE;MVCC=TRUE;AUTO_SERVER=TRUE"
  val file = new File("/home/marlen/Desktop/расходы")

  def main(args: Array[String]): Unit = {
    val expenses = TextParser.parseExpenses(file)
    val db = Db(params = DbParams(name = "expenses", url = dbUrl, username = "", password = ""))
    db.withConnection {
      conn =>
        ExpensesDAO.createExpensesTableIfNeeded(db, conn)
        val rowsModified = ExpensesDAO.saveExpenses(db, conn, expenses)

        println(s"${rowsModified} row(s) inserted")

        val loaded = ExpensesDAO.loadExpenses(db, conn).map(e => e.copy(id = None)).distinct
        if (loaded != expenses) {
          loaded.zipAll(expenses, "", "").map { case (l, e) => if (l != e) println(s"l = ${l} != e: ${e}") }
        }
    }
  }
}
