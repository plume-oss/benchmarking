package com.github.plume.oss
package textfixtures

import java.io.File
import java.util.Collections
import javax.tools.{ JavaCompiler, JavaFileObject, StandardLocation, ToolProvider }
import scala.jdk.CollectionConverters

object JimpleCodeToCpgFixture {

  /** Compiles the source code with debugging info.
    */
  def compileJava(sourceCodeFile: File): Unit = {
    val javac = getJavaCompiler
    val fileManager = javac.getStandardFileManager(null, null, null)
    javac
      .getTask(
        null,
        fileManager,
        null,
        CollectionConverters.SeqHasAsJava(Seq("-g", "-d", sourceCodeFile.getParent)).asJava,
        null,
        fileManager.getJavaFileObjectsFromFiles(CollectionConverters.SeqHasAsJava(Seq(sourceCodeFile)).asJava)
      )
      .call()

    fileManager
      .list(StandardLocation.CLASS_OUTPUT, "", Collections.singleton(JavaFileObject.Kind.CLASS), false)
      .forEach(x => new File(x.toUri).deleteOnExit())
  }

  /** Programmatically obtains the system Java compiler.
    */
  def getJavaCompiler: JavaCompiler =
    Option(ToolProvider.getSystemJavaCompiler) match {
      case Some(javac) => javac
      case None        => throw new RuntimeException("Unable to find a Java compiler on the system!")
    }
}
