package com.github.plume.oss
package textfixtures

import io.joern.dataflowengineoss.queryengine.{EngineConfig, EngineContext}
import io.joern.dataflowengineoss.semanticsloader.{Parser, Semantics}
import io.shiftleft.codepropertygraph.Cpg
import io.shiftleft.codepropertygraph.generated.nodes.{Expression, Literal}
import io.shiftleft.semanticcpg.language._
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import overflowdb.traversal.Traversal

class JimpleDataflowFixture extends AnyFlatSpec with Matchers {

  val semanticsFile: String = "src/test/resources/default.semantics"
  lazy val defaultSemantics: Semantics = Semantics.fromList(new Parser().parseFile(semanticsFile))
  implicit val resolver: ICallResolver = NoResolve
  implicit lazy val engineContext: EngineContext = EngineContext(defaultSemantics, EngineConfig(maxCallDepth = 4))

  val code: String = ""
  lazy val cpg: Cpg = Jimple2CpgTestContext.buildCpgWithDataflow(code)

  def getConstSourceSink(
      methodName: String,
      sourceCode: String = "\"MALICIOUS\"",
      sinkPattern: String = ".*println.*"
  ): (Traversal[Literal], Traversal[Expression]) =
    getMultiFnSourceSink(methodName, methodName, sourceCode, sinkPattern)

  def getMultiFnSourceSink(
      sourceMethodName: String,
      sinkMethodName: String,
      sourceCode: String = "\"MALICIOUS\"",
      sinkPattern: String = ".*println.*"
  ): (Traversal[Literal], Traversal[Expression]) = {
    val sourceMethod = cpg.method(s".*$sourceMethodName").head
    val sinkMethod = cpg.method(s".*$sinkMethodName").head
    def source = sourceMethod.literal.code(sourceCode)
    def sink = sinkMethod.call.name(sinkPattern).argument(1).ast.collectAll[Expression]

    // If either of these fail, then the testcase was written incorrectly or the AST was created incorrectly.
    if (source.size <= 0) {
      fail(s"Could not find source $sourceCode in method $sourceMethodName")
    }
    if (sink.size <= 0) {
      fail(s"Could not find sink $sinkPattern for method $sinkMethodName")
    }

    (source, sink)
  }
}
