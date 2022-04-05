package com.github.plume.oss
package textfixtures

import io.joern.dataflowengineoss.language._
import io.joern.dataflowengineoss.queryengine.{EngineConfig, EngineContext}
import io.joern.dataflowengineoss.semanticsloader.{Parser, Semantics}
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

  def assertIsInsecure(spec: TaintSpec): Assertion =
    assertIsInsecure(spec.source, spec.sink)

  def assertIsSecure(spec: TaintSpec): Assertion =
    assertIsSecure(spec.source, spec.sink)

  /**
    * Makes sure there are flows between the source and the sink
    */
  def assertIsInsecure(source: Traversal[CfgNode], sink: Traversal[CfgNode]): Assertion =
    if (sink.reachableBy(source).isEmpty) {
      fail("[False Negative] Source was not found to taint the sink")
    } else {
      succeed
    }

  /**
    * Makes sure there are no flows between the source and the sink.
    */
  def assertIsSecure(source: Traversal[CfgNode], sink: Traversal[CfgNode]): Assertion =
    if (sink.reachableBy(source).nonEmpty) {
      fail("[False positive] Source was found to taint the sink")
    } else {
      succeed
    }

  def specMainSecretLeakedToPrintln: TaintSpec =
    TaintSpec(
      cpg.fieldAccess.code("Main.secret"),
      cpg.method("main").call(".*println.*").argument(1),
    )
  def specTestInputLeakedToReturn: TaintSpec =
    TaintSpec(
      cpg.method("main").call(".*test.*").argument(1),
      cpg.method("test").methodReturn
    )
  def specFInput1LeakedToInput3: TaintSpec =
    TaintSpec(
      cpg.method("main").call(".*f.*").argument(1),
      cpg.method("main").call(".*f.*").argument(3)
    )
  def specFooInputLeakedToReturn: TaintSpec =
    TaintSpec(
      cpg.method("main").call(".*foo.*").argument(1),
      cpg.method("foo").methodReturn
    )
  def specLeakyMethodInputToReturn: TaintSpec =
    TaintSpec(
      cpg.method("main").call(".*leakyMethod.*").argument(1),
      cpg.method("leakyMethod").methodReturn
    )
  def specDivideLeakToPrintln: TaintSpec =
    TaintSpec(
      cpg.method("main").call(".*divide.*").argument(2),
      cpg.method(".*divide.*").call(".*println.*").argument(1),
    )
  def specFInput1LeakedToReturn: TaintSpec =
    TaintSpec(
      cpg.method("main").call(".*f.*").argument(1),
      cpg.method("f").methodReturn,
    )
  def specScannerLeakToWriteToDisk: TaintSpec =
    TaintSpec(
      cpg.call(".*nextInt.*").astParent.astChildren.isIdentifier,
      cpg.call.methodFullName(".*writeToDisk.*", ".*writeToDB.*").argument(1),
    )
  def specFInput1And2LeakedToReturn: TaintSpec =
    TaintSpec(
      cpg.method("main").call(".*f.*").argumentIndex(1, 2),
      cpg.method("f").methodReturn,
    )
  def specSecureIFLInput1LeakedToReturn: TaintSpec =
    TaintSpec(
      cpg.method("main").call(".*secure_ifl.*").argument(1),
      cpg.method("secure_ifl").methodReturn,
    )
  def specFieldHighLow: TaintSpec =
    TaintSpec(
      cpg.fieldAccess.code(".*high.*"),
      cpg.fieldAccess.code(".*low.*"),
    )
  def specInsecureHighInput1LeakedToReturn: TaintSpec =
    TaintSpec(
      cpg.method("main").call(".*insecure_if_high_n1.*").argument(1),
      cpg.method("insecure_if_high_n1").methodReturn,
    )
  def specListSizeInputLeakedToReturn: TaintSpec =
    TaintSpec(
      cpg.method("main").call(".*listSizeLeak.*").argument(1),
      cpg.method("listSizeLeak").methodReturn,
    )


  case class TaintSpec(source: Traversal[CfgNode], sink: Traversal[CfgNode])

}
