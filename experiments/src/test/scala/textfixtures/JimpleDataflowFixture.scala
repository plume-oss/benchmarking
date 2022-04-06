package com.github.plume.oss
package textfixtures

import ifspec.IFSpecTags
import ifspec.IFSpecTags._

import io.joern.dataflowengineoss.language._
import io.joern.dataflowengineoss.queryengine.{ EngineConfig, EngineContext }
import io.joern.dataflowengineoss.semanticsloader.{ Parser, Semantics }
import io.shiftleft.codepropertygraph.Cpg
import io.shiftleft.codepropertygraph.generated.nodes.CfgNode
import io.shiftleft.semanticcpg.language._
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest._
import overflowdb.traversal.Traversal

import scala.language.implicitConversions

class JimpleDataflowFixture extends AnyFlatSpec with Matchers with BeforeAndAfterAll {

  val semanticsFile: String = "src/test/resources/default.semantics"
  lazy val defaultSemantics: Semantics = Semantics.fromList(new Parser().parseFile(semanticsFile))
  implicit val resolver: ICallResolver = NoResolve
  implicit lazy val engineContext: EngineContext = EngineContext(defaultSemantics, EngineConfig(maxCallDepth = 4))

  val code: String = ""
  lazy val cpg: Cpg = Jimple2CpgTestContext.buildCpgWithDataflow(code)

  override protected def withFixture(test: NoArgTest): Outcome = {
    val outcome = super.withFixture(test)
    val idxToInc = outcome match {
      case Failed(exception) =>
        val falseNegative = exception.getMessage.contains("[False Negative]")
        if (falseNegative)
          Some(FN)
        else
          Some(FP)
      case Succeeded =>
        val truePositive = test.name.contains("[True Positive]")
        if (truePositive)
          Some(TP)
        else
          Some(TN)
      case _ => None
    }

    test.tags.foreach { tag =>
      val arr: Array[Int] = confusionMatrix(tag)
      idxToInc match {
        case Some(idx) => arr(idx) += 1
        case None      =>
      }
    }

    outcome
  }

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

  case class TaintSpec(source: Traversal[CfgNode], sink: Traversal[CfgNode])

}
