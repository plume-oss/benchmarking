package com.github.plume.oss
package ifspec.aliasing

import ifspec.IFSpecTags._
import textfixtures.JimpleDataflowFixture
import io.shiftleft.semanticcpg.language._

class AliasingInterProceduralSecure extends JimpleDataflowFixture {

  behavior of
    """The program creates three internal objects a,b and c.
      |The objects b and c alias.
      |The value of an internal field of a is set to the secret.
      |Then, the same field of c is printed.
      |This does not leak the value of the secret.
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
      |
      |        void update(int val) {
      |            this.val = val;
      |        }
      |    }
      |
      |    static int secret = 42;
      |
      |    public static void main(String[] args) {
      |        A a = new A(1);
      |        A b = new A(1);
      |        A c = b;
      |
      |        doUpdate(a, secret);
      |        System.out.println(c.val);
      |    }
      |
      |    static void doUpdate(A a, int val) {
      |        a.update(val);
      |    }
      |}
      |
      |""".stripMargin

  "[True Negative] The value stored in the field \"secret\" of class \"Main\"" should "not be leaked via " +
    "System.out.println()" taggedAs (Aliasing, ExplicitFlows) in {
    assertIsSecure(
      TaintSpec(
        cpg.fieldAccess.code("Main.secret"),
        cpg.call(".*println.*").argument(1),
      )
    )
  }

}
