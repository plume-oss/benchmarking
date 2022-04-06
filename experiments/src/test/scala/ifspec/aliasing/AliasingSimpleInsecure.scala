package com.github.plume.oss
package ifspec.aliasing

import ifspec.IFSpecTags._
import textfixtures.JimpleDataflowFixture
import io.shiftleft.semanticcpg.language._

class AliasingSimpleInsecure extends JimpleDataflowFixture {

  behavior of
    """This program creates two objects v1, v2 of the same type.
      |Then, it takes input from the user, assigns it to v1.i and lets v2 point to v1.
      |Finally, v2.i is printed, which is the user input v1.i since v1 and v2 are aliased.
      |""".stripMargin

  override val code: String =
    """class Aliasing {
      |
      |    static class A {
      |        int i;
      |    }
      |
      |    static void set(A v1, A v2, int h) {
      |        v1.i = h;
      |    }
      |
      |    static int getNumber() {
      |        return 42;
      |    }
      |
      |    static int test(int i) {
      |        A v1 = new A();
      |        A v2 = new A();
      |        v2 = v1;
      |        set(v1, v2, i);
      |        return v2.i;
      |    }
      |
      |    public static void main(String args[]) throws Exception {
      |        test(getNumber());
      |    }
      |}
      |
      |""".stripMargin

  "[True Positive] The user input is considered high and" should "be leaked to public " +
    "output" taggedAs (Aliasing, ExplicitFlows) in {
    assertIsInsecure(
      TaintSpec(
        cpg.method.call(".*test.*").argument(1),
        cpg.method("test").methodReturn,
      )
    )
  }

}
