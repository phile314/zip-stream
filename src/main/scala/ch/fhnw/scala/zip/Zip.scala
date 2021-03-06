package ch.fhnw.scala.zip


import java.nio.charset.Charset
import java.util.zip.CRC32

import com.github.nscala_time.time.Imports._
import scodec.bits._

import scalaz._
import scalaz.stream._

/**
  * Creates zip files conforming to the https://pkware.cachefly.net/webdocs/casestudies/APPNOTE.TXT specification.
  *
  */
object Zip {

  case class ZipOptions()
  val DefaultOptions = ZipOptions()

  case class ZipEntry[M[_]](header: ZipEntryHeader, data: ZipData[M])

  case class ZipEntryHeader(name: String, modTime: DateTime, size: Option[Long])

  abstract sealed class ZipData[M[_]]()

  case class ZipDataByteVector[M[_]](bytes: ByteVector) extends ZipData[M]

  /**
    * Read the file content from the given process.
    */
  case class ZipDataSource[M[_]](process: Process[M, ByteVector]) extends ZipData[M]

  /**
    * Encode the zip entries as byte vectors.
    * @param in The zip entry source.
    * @tparam M The effect monad.
    */
  def encode[M[_]](in: Process[M, ZipEntry[M]], options: ZipOptions = DefaultOptions): Process[M, ByteVector] = {
    new Zip[M]().encode(in)
  }

}

private class Zip[M[_]] {

  import Zip._

  private val madeByZipVersion = 45
  private val requiredZipVersion = 45

  var entryDictionary: List[ByteVector] = Nil
  var offset:Int = 0

  def encode(in: Process[M, ZipEntry[M]]): Process[M, ByteVector] = {
    in.flatMap {
      case ZipEntry(header, data) => {
        val entryOffset = offset
        data match {
          case ZipDataByteVector(bytes) => {
            val crc32 = crc.crc32(bytes.bits).bytes.reverse
            entryDictionary = centralDirectoryEntry(header, entryOffset, crc32, bytes.size.toInt) :: entryDictionary
            localFileHeader(header) ++ Process.suspend{
              offset += bytes.length.toInt
              Process.emit(bytes)
            } ++ dataDescriptor(bytes.size.toInt, crc32)
          }
          case ZipDataSource(p) => {
            var size = 0;
            val crc32 = new CRC32()
            val digest = new Function[ByteVector, ByteVector] {
              override def apply (v1: ByteVector): ByteVector = {
                offset += v1.length.toInt
                size += v1.length.toInt
                crc32.update(v1.toArray)
                v1
              }
            };
            localFileHeader(header) ++ p.map(digest).onComplete({
              val crc = int4(crc32.getValue.toInt)
              entryDictionary = centralDirectoryEntry(header, entryOffset, crc, size) :: entryDictionary
              dataDescriptor(size, crc)
            })
          }
        }
      }
    } onComplete(centralDirectory(entryDictionary.reverse))
  }

  private def centralDirectoryEntry(header: ZipEntryHeader, localHeaderOffset: Int, crc:ByteVector, size:Int):ByteVector = {
    val nameBytes = header.name.getBytes(Charset.forName("UTF-8"));

    hex("0x02014b50") ++
      int2(madeByZipVersion) ++
      int2(requiredZipVersion) ++
      bin("0000100000001000") ++
      hex("0000") ++
      toDOSTime(header.modTime) ++
      crc ++
      int4(size) ++
      int4(size) ++
      int2(nameBytes.length) ++
      int2(0) ++
      int2(0) ++ // comment length
      int2(0) ++ // disk number
      int2(0) ++ // internal file attributes
      int4(0) ++ //external file attributes
      int4(localHeaderOffset) ++
      ByteVector(nameBytes)

  }
  private def centralDirectory(entries:List[ByteVector]):Process[M, ByteVector] = {
    var startOffset = 0
    Process.suspend {
      startOffset = offset;
      val procs = entries.map(e => Process.suspend{
        offset += e.length.toInt
        Process.emit(e)
      }).fold(Process.empty)((x, y) => x ++ y)
      procs

    } ++ Process.suspend {
      val endOffset = offset;
      endDirectory(entries.size, startOffset, endOffset - startOffset)
    }
  }

  private def endDirectory(entryCount:Int, cdOff: Int, cdLen:Int):Process[M, ByteVector] = {
    Process.suspend {
      val zip64End = hex("0x06064b50") ++
        int8(44) ++
        int2(madeByZipVersion) ++
        int2(requiredZipVersion) ++
        int4(0) ++ // this disk
        int4(0) ++ // central disk
        int8(entryCount.toLong) ++
        int8(entryCount.toLong) ++
        int8(cdLen.toLong) ++ // central directory length
        int8(cdOff.toLong)
      val zip64Locator = hex("0x07064b50") ++
        int4(0) ++
        int8(cdOff.toLong + cdLen.toLong) ++
        int4(1)

      val end = hex("0x06054b50") ++
        int2(0) ++
        int2(0) ++
        int2(entryCount) ++
        int2(entryCount) ++
        int4(cdLen) ++
        int4(cdOff) ++
        int2(0)

      Process.emit(zip64End ++ zip64Locator ++ end)
    }
  }

  private def dataDescriptor(size:Int, crc32: ByteVector):Process[M, ByteVector] = {
    val result = crc32 ++ int4(size) ++ int4(size)
    Process.suspend {
      offset += result.length.toInt
      Process.emit(result)
    }
  }

  private def localFileHeader(header: ZipEntryHeader):Process[M, ByteVector] = {
    val nameBytes = header.name.getBytes(Charset.forName("UTF-8"));
      // signature (4B)
    val result = hex("0x04034b50") ++
      // version needed (2B)
      int2(requiredZipVersion) ++
      // general purpose flags (2B)
      bin("0000100000001000") ++
      // compression method (none) (2B)
      hex("0000") ++
      // last mod (2 + 2B)
      toDOSTime(header.modTime) ++
      // crc32, comp. + uncomp. size (4 + 4 + 4 B)
      // set to zero here, see data descriptor for values
      ByteVector.fill(12)(0) ++
      // filename length (2B)
      int2(nameBytes.length) ++
      // extra field length (2B)
      hex("0000") ++
      // filename
      ByteVector(nameBytes) ++
      // extra field
      ByteVector.empty

    Process.suspend {
      offset += result.length.toInt
      Process.emit(result)
    }
  }



  private def toDOSTime(datetime:DateTime):ByteVector = {
    // See e.g. https://msdn.microsoft.com/en-us/library/windows/desktop/ms724247(v=vs.85).aspx
    val dt = datetime.toDateTime(DateTimeZone.UTC)
    val time =
      BitVector.fromInt(dt.getHourOfDay, 5, ByteOrdering.LittleEndian) ++
        BitVector.fromInt(dt.getMinuteOfHour, 6, ByteOrdering.LittleEndian) ++
        BitVector.fromInt(dt.getSecondOfMinute / 2, 5, ByteOrdering.LittleEndian)
    val date =
      BitVector.fromInt(dt.getYear - 1980, 7, ByteOrdering.LittleEndian) ++
        BitVector.fromInt(dt.getMonthOfYear, 4, ByteOrdering.LittleEndian) ++
        BitVector.fromInt(dt.getDayOfMonth, 5, ByteOrdering.LittleEndian)
    (time.bytes.reverse ++ date.bytes.reverse)
  }

  private def bin(s:String):ByteVector = {
    ByteVector.fromValidBin(s).reverse
  }

  private def hex(s:String):ByteVector = {
    ByteVector.fromValidHex(s).reverse
  }

  private def int2(x:Int):ByteVector = {
    ByteVector.fromInt(x, 2, ByteOrdering.LittleEndian)
  }

  private def int4(x:Int):ByteVector = {
    ByteVector.fromInt(x, 4, ByteOrdering.LittleEndian)
  }

  private def int8(x:Long):ByteVector = {
    ByteVector.fromLong(x, 8, ByteOrdering.LittleEndian)
  }
}

