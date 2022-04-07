package com.github.plume.oss
package ifspec.array

import ifspec.IFSpecTags._
import textfixtures.JimpleDataflowFixture
import io.shiftleft.semanticcpg.language._

class ArrayIndexSensitivitySecure extends JimpleDataflowFixture {

  behavior of
    """The given method has one parameter. It creates an int array of length 2.
      |It stores its parameter at index 0 and returns the value at index 1.
      |""".stripMargin

  override val code: String =
    """class program {
      |    public static int foo(int h) {
      |        int[] a = new int[2];
      |        a[0] = h;
      |        return a[1];
      |    }
      |}
      |
      |""".stripMargin

  "[True Negative] The parameter value" should "not flow to the return value" taggedAs (Arrays, ExplicitFlows) in {
    assertIsSecure(
      TaintSpec(
        cpg.method("foo").parameter,
        cpg.method("foo").ast.isReturn
      )
    )
  }

}
