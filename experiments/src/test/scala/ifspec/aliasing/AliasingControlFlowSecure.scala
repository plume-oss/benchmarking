package com.github.plume.oss
package ifspec.aliasing

import ifspec.IFSpecTags._
import textfixtures.JimpleDataflowFixture
import io.shiftleft.semanticcpg.language._

class AliasingControlFlowSecure extends JimpleDataflowFixture {

  behavior of
    """The program creates two internal objects a and b that alias.
      |If the secret is equal to 42, the value of an internal field of a is set to 2.
      |Else, the same field is also set to 2.
      |Finally, the value of the same internal field of b is printed.
      |This does not leak any information about the secret, as the field will have the same
      |value, not matter what the secret value is.
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
      |    static private int secret = 42;
      |
      |    public static void main(String[] args) {
      |        A a = new A(1);
      |        A b = a;
      |
      |        if (secret == 42) {
      |            a.val = 2;
      |        } else {
      |            a.val = 2;
      |        }
      |
      |        System.out.println(b.val);
      |    }
      |}
      |
      |""".stripMargin

  "[Secure] The value stored in the field \"secret\" of class \"Main\"" should "not be leaked via System.out.println()" taggedAs (Aliasing, ImplicitFlows) in {
    assertIsSecure(
      TaintSpec(
        cpg.fieldAccess.code("Main.secret"),
        cpg.method("main").call(".*println.*").argument(1),
      )
    )
  }

}
