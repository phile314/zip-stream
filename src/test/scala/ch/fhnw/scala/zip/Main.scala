package ch.fhnw.scala.zip

import ch.fhnw.scala.zip.Zip._

import scalaz._
import scalaz.stream._
import scalaz.stream.io._
import com.github.nscala_time.time.Imports._
import scodec.bits._

import scalaz.concurrent.Task

/**
  * Small test app creating a simple zip file.
  */
object Main extends App {
    val content: Process[Task, ZipEntry[Task]] = Process.emit(ZipEntry(ZipEntryHeader("Test.txt", DateTime.now(), None), ZipDataByteVector(ByteVector.encodeUtf8("sfasdf").right.get)))

    val zipped = Zip.encode(content)

    val sink = fileChunkW("test.zip")

    zipped.to(sink).run.unsafePerformSync


}
