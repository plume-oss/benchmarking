package com.github.plume.oss
package ifspec.simple

import ifspec.IFSpecTags._
import textfixtures.JimpleDataflowFixture
import io.shiftleft.semanticcpg.language._

class CallContext extends JimpleDataflowFixture {

  behavior of
    """This program copies its parameter into a local variable using a helper method.
      |Afterwards it uses that helper method to copy a constant value and returns that constant value.
      |""".stripMargin

  override val code: String =
    """class program {
      |
      |    static int foo(int h) {
      |        int y = id(h);
      |        int x = 0;
      |        return id(x);
      |    }
      |
      |    static int id(int x) {
      |      return x;
      |    }
      |
      |    public static void main (String [] args) {
      |        foo(randInt());
      |    }
      |
      |    /** Helper method to obtain a random boolean */
      |    static boolean randBool() {
      |        return true;
      |    }
      |    /** Helper method to obtain a random integer */
      |    static int randInt() {
      |        return 42;
      |    }
      |
      |}
      |
      |""".stripMargin

  "[Secure] The parameter of the method 'foo'" should "not flow to the return value of the method 'foo'" taggedAs (Simple, ExplicitFlows) in {
    assertIsSecure(
      TaintSpec(
        cpg.method("main").call(".*foo.*").argument(1),
        cpg.method("foo").methodReturn
      )
    )
  }

}
