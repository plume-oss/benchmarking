package com.github.plume.oss
package ifspec.classinitializer

import ifspec.IFSpecTags._
import textfixtures.JimpleDataflowFixture

import io.shiftleft.semanticcpg.language._

class StaticInitializersArrayAccessInsecure extends JimpleDataflowFixture {

  behavior of
    """When the class A is initialized, the secret value is stored in an array.
      |After performing an operation that initialized the class A, the secret value is
      |read from the array and leaked by printing it.
      |""".stripMargin

  override val code: String =
    """class Main {
      |
      |    private static String secret = "secret";
      |    private static String[] vals = { "a", "b", "c" };
      |
      |    static class A {
      |        static {
      |            vals[0] = secret;
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

  "[True Positive] The value stored in the field \"secret\" of class \"Main\"" should "not be leaked via" +
    " System.out.println()." taggedAs(Arrays, ClassInitializer, ExplicitFlows) in {
    assertIsInsecure(TaintSpec(
      cpg.fieldAccess.code(".*secret.*"),
      cpg.call(".*println.*").argument(1)
    ))
  }

}
