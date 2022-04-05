package com.github.plume.oss
package ifspec.highconditional

import ifspec.IFSpecTags._
import textfixtures.JimpleDataflowFixture

import io.shiftleft.semanticcpg.language._

class SimpleConditionalAssignmentEqual extends JimpleDataflowFixture {

  behavior of
    """The program consists of a single class simpleConditionalAssignmentEqual that has a field secret and a single
      |main method. The main method assigns a string to a variable value depending on the secret. The assigned value is
      |equal is both branches. Afterwards the value is written to standard output.
      |""".stripMargin

  override val code: String =
    """
      |class simpleConditionalAssignmentEqual {
      |	private static boolean secret = true;
      |
      |	public static void main(String[] args) {
      |		test();
      |	}
      |
      |	public static int test() {
      |		int value;
      |
      |		if (secret) {
      |			value = 1;
      |		} else {
      |			value = 1;
      |		}
      |
      |		return value;
      |	}
      |
      |}
      |
      |""".stripMargin

  "[Secure] The secret in simpleConditionalAssignmentEqual" should "not be passed to " +
    "the command line." taggedAs(HighConditional, ImplicitFlows) in {
    assertIsSecure(
      TaintSpec(
        cpg.fieldAccess.code(".*secret.*"),
        cpg.method("test").methodReturn
      )
    )
  }

}
