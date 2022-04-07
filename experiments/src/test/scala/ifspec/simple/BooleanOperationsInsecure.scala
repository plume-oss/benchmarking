package com.github.plume.oss
package ifspec.simple

import ifspec.IFSpecTags._
import textfixtures.JimpleDataflowFixture
import io.shiftleft.semanticcpg.language._

class BooleanOperationsInsecure extends JimpleDataflowFixture {

  behavior of
    """This method does some computation on that value and returns that value.
      |The computation yields 'true', if the secret is 'true'.
      |""".stripMargin

  override val code: String =
    """class BooleanOperations {
      |	public static boolean leakyMethod(boolean high) {
      |		boolean ret;
      |		ret = (high && true);
      |		return ret;
      |	}
      |}
      |
      |""".stripMargin

  "[True Positive] There " should "be any flow from the parameter to the return value of the " +
    "method" taggedAs (Simple, ExplicitFlows) in {
    assertIsInsecure(
      TaintSpec(
        cpg.method("leakyMethod").parameter,
        cpg.method("leakyMethod").block.ast.isReturn
      )
    )
  }

}
