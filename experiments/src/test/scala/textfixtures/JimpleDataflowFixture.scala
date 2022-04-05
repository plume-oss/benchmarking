package com.github.plume.oss
package textfixtures

import io.joern.dataflowengineoss.language._
import io.joern.dataflowengineoss.queryengine.{ EngineConfig, EngineContext }
import io.joern.dataflowengineoss.semanticsloader.{ Parser, Semantics }
import io.shiftleft.codepropertygraph.Cpg
import io.shiftleft.codepropertygraph.generated.nodes.CfgNode
import io.shiftleft.semanticcpg.language._
import org.scalatest.Assertion
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import overflowdb.traversal.Traversal

import scala.language.implicitConversions

class JimpleDataflowFixture extends AnyFlatSpec with Matchers {

  val semanticsFile: String = "src/test/resources/default.semantics"
  lazy val defaultSemantics: Semantics = Semantics.fromList(new Parser().parseFile(semanticsFile))
  implicit val resolver: ICallResolver = NoResolve
  implicit lazy val engineContext: EngineContext = EngineContext(defaultSemantics, EngineConfig(maxCallDepth = 4))

  val code: String = ""
  lazy val cpg: Cpg = Jimple2CpgTestContext.buildCpgWithDataflow(code)

  def assertIsInsecure(spec: TaintSpec): Assertion = {
    val (source, sink) = getSourceSinkPair(spec.source, spec.sink)
    assertIsInsecure(source, sink)
  }

  def assertIsSecure(spec: TaintSpec): Assertion = {
    if (spec.source().size <= 0 || spec.sink().size <= 0) return succeed
    val (source, sink) = getSourceSinkPair(spec.source, spec.sink)
    assertIsSecure(source, sink)
  }

  def assertIsInsecure(source: Traversal[CfgNode], sink: Traversal[CfgNode]): Assertion =
    if (sink.reachableBy(source).nonEmpty) {
      fail("[False Negative] Source was not found to taint the sink")
    } else {
      succeed
    }

  def assertIsSecure(source: Traversal[CfgNode], sink: Traversal[CfgNode]): Assertion =
    if (sink.reachableBy(source).isEmpty) {
      fail("[False positive] Source was found to taint the sink")
    } else {
      succeed
    }

  def getSourceSinkPair(source: () => Traversal[CfgNode],
                        sink: () => Traversal[CfgNode]): (Traversal[CfgNode], Traversal[CfgNode]) = {
    if (source().size <= 0) {
      fail(s"Could not find source")
    }
    if (sink().size <= 0) {
      fail(s"Could not find sink")
    }
    (source(), sink())
  }

  val specMainSecretLeakedToPrintln: TaintSpec =
    TaintSpec(
      () => cpg.fieldAccess.code("Main.secret"),
      () => cpg.method("main").call(".*println.*").argument(1),
    )
  val specTestInputLeakedToReturn: TaintSpec =
    TaintSpec(
      () => cpg.method("main").call(".*test.*").argument(1),
      () => cpg.method("test").methodReturn
    )
  val specFInput1LeakedToInput3: TaintSpec =
    TaintSpec(
      () => cpg.method("main").call(".*f.*").argument(1),
      () => cpg.method("main").call(".*f.*").argument(3)
    )
  val specFooInputLeakedToReturn: TaintSpec =
    TaintSpec(
      () => cpg.method("main").call(".*foo.*").argument(1),
      () => cpg.method("foo").methodReturn
    )
  val specLeakyMethodInputToReturn: TaintSpec =
    TaintSpec(
      () => cpg.method("main").call(".*leakyMethod.*").argument(1),
      () => cpg.method("leakyMethod").methodReturn
    )
  val specDivideLeakToPrintln: TaintSpec =
    TaintSpec(
      () => cpg.method("main").call(".*divide.*").argument(2),
      () => cpg.method(".*divide.*").call(".*println.*").argument(1),
    )
  val specFInput1LeakedToReturn: TaintSpec =
    TaintSpec(
      () => cpg.method("main").call(".*f.*").argument(1),
      () => cpg.method("f").methodReturn,
    )
  val specScannerLeakToWriteToDisk: TaintSpec =
    TaintSpec(
      () => cpg.call(".*nextInt.*").astParent.astChildren.isIdentifier,
      () => cpg.call.methodFullName(".*writeToDisk.*", ".*writeToDB.*").argument(1),
    )

  case class TaintSpec(source: () => Traversal[CfgNode], sink: () => Traversal[CfgNode])

}
