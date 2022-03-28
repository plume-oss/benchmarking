package com.github.plume.oss

import net.jpountz.lz4.LZ4BlockOutputStream
import org.apache.commons.compress.archivers.tar.{TarArchiveEntry, TarArchiveOutputStream}
import org.apache.commons.compress.compressors.xz.XZCompressorOutputStream
import org.apache.commons.compress.compressors.zstandard.ZstdCompressorOutputStream

import java.io.BufferedOutputStream
import java.nio.file.{Files, Path}
import java.util.zip.{ZipEntry, ZipOutputStream}
import scala.util.Using

object CompressionUtil {
  def zip(in: Path, out: Path): Long =
    Using.resource(new ZipOutputStream(Files.newOutputStream(out))) { zip =>
      zip.putNextEntry(new ZipEntry(in.toString))
      Files.copy(in, zip)
      zip.closeEntry()
      out.toFile.length()
    }

  def zstd(in: Path, out: Path): Long =
    Using.resources(Files.newInputStream(in),
      new ZstdCompressorOutputStream(new BufferedOutputStream(Files.newOutputStream(out)))) {
      case (fis, lz4Out) =>
        val buffer = new Array[Byte](1024)
        Iterator
          .continually(fis.read(buffer))
          .takeWhile(_ != -1)
          .foreach(_ => lz4Out.write(buffer))
        out.toFile.length()
    }

  def lz4(in: Path, out: Path): Long =
    Using.resources(Files.newInputStream(in),
      new LZ4BlockOutputStream(new BufferedOutputStream(Files.newOutputStream(out)))) {
      case (fis, gzipOut) =>
        val buffer = new Array[Byte](1024)
        Iterator
          .continually(fis.read(buffer))
          .takeWhile(_ != -1)
          .foreach(_ => gzipOut.write(buffer))
        out.toFile.length()
    }

  def tar(in: Path, out: Path): Long =
    Using.resources(Files.newInputStream(in), new TarArchiveOutputStream(Files.newOutputStream(out))) {
      case (fis, tarOut) =>
        val entry = new TarArchiveEntry(in.toString)
        entry.setSize(in.toFile.length())
        tarOut.putArchiveEntry(entry)
        val buffer = new Array[Byte](256)
        Iterator
          .continually(fis.read(buffer))
          .takeWhile(_ != -1)
          .foreach(_ => tarOut.write(buffer))
        tarOut.closeArchiveEntry()
        out.toFile.length()
    }

  def xz(in: Path, out: Path): Long =
    Using.resources(Files.newInputStream(in),
      new XZCompressorOutputStream(new BufferedOutputStream(Files.newOutputStream(out)))) {
      case (fis, xzOut) =>
        val buffer = new Array[Byte](1024)
        Iterator
          .continually(fis.read(buffer))
          .takeWhile(_ != -1)
          .foreach(_ => xzOut.write(buffer))
        out.toFile.length()
    }
}
