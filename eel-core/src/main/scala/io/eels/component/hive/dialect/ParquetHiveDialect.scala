package io.eels.component.hive.dialect

import com.typesafe.config.ConfigFactory
import com.typesafe.scalalogging.slf4j.StrictLogging
import io.eels.component.avro.{AvroRecordFn, AvroSchemaFn}
import io.eels.component.hive.{HiveDialect, HiveWriter}
import io.eels.component.parquet.{ParquetIterator, ParquetLogMute, RollingParquetWriter}
import io.eels.{InternalRow, Schema}
import org.apache.hadoop.fs.{FileSystem, Path}

object ParquetHiveDialect extends HiveDialect with StrictLogging {

  private val config = ConfigFactory.load()

  override def iterator(path: Path, schema: Schema, columns: Seq[String])
                       (implicit fs: FileSystem): Iterator[InternalRow] = new Iterator[InternalRow] {
    ParquetLogMute()

    lazy val iter = ParquetIterator(path, columns)
    override def hasNext: Boolean = iter.hasNext
    override def next(): InternalRow = iter.next
  }

  override def writer(schema: Schema, path: Path)
                     (implicit fs: FileSystem): HiveWriter = {
    ParquetLogMute()
    logger.debug(s"Creating parquet writer for $path")

    val avroSchema = AvroSchemaFn.toAvro(schema)
    val writer = RollingParquetWriter(path, avroSchema)

    new HiveWriter {
      override def close(): Unit = writer.close()
      override def write(row: InternalRow): Unit = {
        val record = AvroRecordFn.toRecord(row, avroSchema, schema, config)
        logger.trace(record.toString)
        writer.write(record)
      }
    }
  }
}
