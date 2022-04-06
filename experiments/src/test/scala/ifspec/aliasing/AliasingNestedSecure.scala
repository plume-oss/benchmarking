package com.github.plume.oss
package ifspec.aliasing

import ifspec.IFSpecTags._
import textfixtures.JimpleDataflowFixture
import io.shiftleft.semanticcpg.language._

class AliasingNestedSecure extends JimpleDataflowFixture {

  behavior of
    """The program creates an internal object, that has a field that contains another
      |object containing the secret value.
      |""".stripMargin

  override val code: String =
    """class Main {
      |
      |    static class A {
      |        B b;
      |
      |        A(B b) {
      |            this.b = b;
      |        }
      |    }
      |
      |    static class B {
      |        int val;
      |
      |        B(int val) {
      |            this.val = val;
      |        }
      |    }
      |
      |    static int secret = 42;
      |
      |    public static void main(String[] args) {
      |        B b = new B(1);
      |        A a = new A(b);
      |
      |        b.val = secret;
      |        a.b = new B(1);
      |
      |        System.out.println(a.b.val);
      |    }
      |}
      |
      |""".stripMargin

  "[True Negative] The value stored in the field \"secret\" of class \"Main\"" should "not be leaked via " +
    "System.out.println()" taggedAs (Aliasing, ExplicitFlows) in {
    assertIsSecure(
      TaintSpec(
        cpg.fieldAccess.code("Main.secret"),
        cpg.method("main").call(".*println.*").argument(1),
      )
    )
  }

}
