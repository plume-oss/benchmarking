package com.github.plume.oss
package ifspec.classinitializer

import ifspec.IFSpecTags._
import textfixtures.JimpleDataflowFixture

import io.shiftleft.semanticcpg.language._

class StaticInitializersArrayAccessSecure extends JimpleDataflowFixture {

  behavior of
    """When the class A is initialized, the secret value is stored in an array.
      |After performing an operation that initialized the class A, the another value is
      |read from the array and printed. Hence, the secret value is not leaked.
      |""".stripMargin

  override val code: String =
    """class Main {
      |
      |    private static String secret = "secret";
      |    private static String[] vals = { "a", "b", "c" };
      |
      |    static class A {
      |        static {
      |            vals[1] = secret;
      |        }
      |
      |        void leak() {
      |            System.out.println(vals[0]);
      |        }
      |    }
      |
      |    public static void main(String[] args) {
      |        A a = new A();
      |        a.leak();
      |    }
      |
      |}
      |
      |""".stripMargin

  "[True Negative] The value stored in the field \"secret\" of class \"Main\"" should "not be leaked via " +
    "System.out.println()." taggedAs(Arrays, ClassInitializer, ExplicitFlows) in {
    assertIsSecure(TaintSpec(
      cpg.fieldAccess.code(".*secret.*"),
      cpg.method("leak").call(".*println.*").argument
    ))
  }

}
