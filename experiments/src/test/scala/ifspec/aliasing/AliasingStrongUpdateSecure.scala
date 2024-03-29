package com.github.plume.oss
package ifspec.aliasing

import ifspec.IFSpecTags._
import textfixtures.JimpleDataflowFixture
import io.shiftleft.semanticcpg.language._

class AliasingStrongUpdateSecure extends JimpleDataflowFixture {

  behavior of
    """The program prints a field from an object that contains the secret value
      |at some point of the execution, but is changed before the print operation happens.
      |""".stripMargin

  override val code: String =
    """class Main {
      |
      |    static class A {
      |        int val;
      |
      |        A(int val) {
      |            this.val = val;
      |        }
      |    }
      |
      |    static int secret = 42;
      |
      |    public static void main(String[] arg) {
      |        A a = new A(secret);
      |        A b = new A(5);
      |        A c = b;
      |
      |        b = a;
      |
      |        a.val = 2;
      |
      |        System.out.println(c.val);
      |    }
      |}
      |
      |""".stripMargin

  "[True Negative] The value stored in the field \"secret\" of class \"Main\"" should "not be leaked via " +
    "System.out.println()" taggedAs (Aliasing, ImplicitFlows) in {
    assertIsSecure(
      TaintSpec(
        cpg.fieldAccess.code("Main.secret"),
        cpg.call(".*println.*").argument(1),
      )
    )
  }

}
