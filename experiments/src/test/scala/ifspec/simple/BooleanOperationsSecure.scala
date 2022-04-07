package com.github.plume.oss
package ifspec.simple

import ifspec.IFSpecTags._
import textfixtures.JimpleDataflowFixture
import io.shiftleft.semanticcpg.language._

class BooleanOperationsSecure extends JimpleDataflowFixture {

  behavior of
    """This method does some computation on that value and returns that value.
      |The computation always yields 'true', hence the return value is always 'true'.
      |""".stripMargin

  override val code: String =
    """class BooleanOperations {
      |	public static boolean leakyMethod(boolean high) {
      |		boolean ret;
      |		ret = (high || true) || (high || false);
      |		return ret;
      |	}
      |}
      |
      |""".stripMargin

  "[True Negative] There " should "not be any flow from the parameter to the return value of the " +
    "method" taggedAs (Simple, ExplicitFlows) in {
    assertIsSecure(
      TaintSpec(
        cpg.method("leakyMethod").parameter,
        cpg.method("leakyMethod").ast.isReturn
      )
    )
  }

}
