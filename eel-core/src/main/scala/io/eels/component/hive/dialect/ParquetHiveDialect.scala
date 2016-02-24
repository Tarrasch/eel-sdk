package io.eels.component.hive.dialect

import com.typesafe.scalalogging.slf4j.StrictLogging
import io.eels.component.avro.{AvroRecordFn, AvroSchemaFn}
import io.eels.component.hive.{HiveDialect, HiveWriter}
import io.eels.component.parquet.{ParquetIterator, ParquetLogMute, RollingParquetWriterSupport}
import io.eels.{FrameSchema, InternalRow}
import org.apache.hadoop.fs.{FileSystem, Path}

object ParquetHiveDialect extends HiveDialect with StrictLogging with RollingParquetWriterSupport {

  override def iterator(path: Path, schema: FrameSchema, columns: Seq[String])
                       (implicit fs: FileSystem): Iterator[InternalRow] = new Iterator[InternalRow] {
    ParquetLogMute()

    lazy val iter = ParquetIterator(path, columns)
    override def hasNext: Boolean = iter.hasNext
    override def next(): InternalRow = iter.next
  }

  override def writer(sourceSchema: FrameSchema, targetSchema: FrameSchema, path: Path)
                     (implicit fs: FileSystem): HiveWriter = {
    ParquetLogMute()
    logger.debug(s"Creating parquet writer for $path")

    val avroSchema = AvroSchemaFn.toAvro(targetSchema)
    val writer = createRollingParquetWriter(path, avroSchema)

    new HiveWriter {
      override def close(): Unit = writer.close()
      override def write(row: InternalRow): Unit = {
        val record = AvroRecordFn.toRecord(row, avroSchema, sourceSchema, config)
        writer.write(record)
      }
    }
  }
}
