package com.github.plume.oss
package ifspec.simple

import ifspec.IFSpecTags._
import textfixtures.JimpleDataflowFixture
import io.shiftleft.semanticcpg.language._

class DirectAssignmentLeak extends JimpleDataflowFixture {

  behavior of
    """The method 'f' copies the value of its first parameter into its second parameter and returns the second parameter.
      |""".stripMargin

  override val code: String =
    """class Eg1 {
      |	public static void main(String args[]) {
      |		f(randInt(), randInt());
      |	}
      |
      |	public static int f(int h, int l) {
      |		l = h;
      |		return l;
      |	}
      |
      |	/** Helper methot to obtain a random integer */
      |	static int randInt() {
      |		return 42;
      |	}
      |
      |}
      |
      |""".stripMargin

  "[Insecure] The first parameter of method 'f'" should "flow into the return value of method 'f'" taggedAs (Simple, ExplicitFlows) in {
    assertIsInsecure(
      TaintSpec(
        cpg.method("main").call(".*f.*").argument(1),
        cpg.method("f").methodReturn,
      )
    )
  }

}
