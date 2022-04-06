package com.github.plume.oss
package ifspec

import org.scalatest.Tag

import scala.collection.mutable

object IFSpecTags {
  object Aliasing extends Tag("Aliasing")
  object Arrays extends Tag("Arrays")
  object Casting extends Tag("Casting")
  object ClassInitializer extends Tag("Class Initializer")
  object HighConditional extends Tag("High Conditional")
  object ImplicitFlows extends Tag("Implicit Flows")
  object Exceptions extends Tag("Exceptions")
  object ExplicitFlows extends Tag("Explicit Flows")
  object Library extends Tag("Library")
  object Simple extends Tag("Simple")

  def TAGS = Seq(
    ExplicitFlows.name,
    ImplicitFlows.name,
    Aliasing.name,
    Arrays.name,
    HighConditional.name,
    Library.name,
    Simple.name,
    Exceptions.name,
    Casting.name,
    ClassInitializer.name,
  )

  val confusionMatrix = mutable.Map.empty[String, Array[Int]]
  val FP = 0
  val TP = 1
  val TN = 2
  val FN = 3

  TAGS.foreach { tag =>
    confusionMatrix.put(tag, Array.ofDim[Int](4))
  }

  private def finalResults(): Unit = {
    println(
      "\\textbf{Category} & \\textbf{No. Tests} & \\textbf{FP} & \\textbf{TP} & \\textbf{TN} & \\textbf{FN} \\\\ \\hline"
    )
    TAGS.foreach { tag =>
      val m = confusionMatrix(tag)
      println(s"$tag & ${m.sum} & ${m(FP)} & ${m(TP)} & ${m(TN)} & ${m(FN)} \\\\")
    }
    println("\\hline")
    val em = confusionMatrix(ExplicitFlows.name)
    val im = confusionMatrix(ImplicitFlows.name)
    val totalTests = em.sum + im.sum
    println(
      s"\\textbf{Total} & " +
        s"\\textbf{$totalTests} & " +
        s"\\textbf{${em(FP) + im(FP)}} & " +
        s"\\textbf{${em(TP) + im(TP)}} & " +
        s"\\textbf{${em(TN) + im(TN)}} & " +
        s"\\textbf{${em(FN) + im(FN)}}"
    )

    println(s"""
               |Total accuracy: ${(em(TP) + im(TP) + em(TN) + im(TN) + 0.0) / totalTests * 100.0}%
               |""".stripMargin)
  }

  sys.addShutdownHook(finalResults())
}
