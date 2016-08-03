package io.eels.component.jdbc

import io.eels.RowListener
import io.eels.schema.Field
import io.eels.schema.FieldType
import io.eels.schema.Precision
import io.eels.schema.Schema
import io.kotlintest.specs.StringSpec
import java.sql.DriverManager

class ResultsetPartTest : StringSpec() {

  init {

    Class.forName("org.h2.Driver")
    val conn = DriverManager.getConnection("jdbc:h2:mem:ResultsetPartTest")
    conn.createStatement().executeUpdate("create table mytable (a integer, b bit, c bigint)")
    conn.createStatement().executeUpdate("insert into mytable (a,b,c) values ('1','2','3')")
    conn.createStatement().executeUpdate("insert into mytable (a,b,c) values ('4','5','6')")

    "ResultsetPart should publish fields in schema order" {

      val schema = Schema(
          Field("c", FieldType.Int, true, Precision(10), signed = true),
          Field("b", FieldType.Boolean, true, Precision(1), signed = true),
          Field("a", FieldType.Long, true, Precision(19), signed = true)
      )

      val stmt = conn.createStatement()
      val rs = stmt.executeQuery("select * from mytable")
      val data = ResultsetPart(rs, stmt, conn, schema, RowListener.Noop).data().toBlocking().first()
      data.values shouldBe listOf(3L, true, 1)
    }
  }
}