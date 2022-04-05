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
      |        int [] a = new int [2];
      |        a[0] = h;
      |        return a[1];
      |    }
      |
      |    public static void main(String[] args){
      |    	program.foo(1);
      |    }
      |}
      |
      |""".stripMargin

  "[Secure] The parameter value" should "not flow to the return value" taggedAs (Arrays, ImplicitFlows) in {
    assertIsSecure(
      TaintSpec(
        cpg.method("main").call(".*foo.*").argument(1),
        cpg.method("foo").methodReturn
      )
    )
  }

}
