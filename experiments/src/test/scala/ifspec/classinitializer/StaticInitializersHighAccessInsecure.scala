package com.github.plume.oss
package ifspec.classinitializer

import ifspec.IFSpecTags._
import textfixtures.JimpleDataflowFixture

import io.shiftleft.semanticcpg.language._

class StaticInitializersHighAccessInsecure extends JimpleDataflowFixture {

  behavior of
    """The program prints the secret Value when the class A is initialized.
      |""".stripMargin

  override val code: String =
    """class Main {
      |
      |    private static String secret = "secret";
      |
      |    static class A {
      |        static {
      |            System.out.println(secret);
      |        }
      |
      |        int add(int a, int b) {
      |            return a + b;
      |        }
      |    }
      |
      |    public static void main(String[] args) {
      |        A a = new A();
      |        a.add(1, 2);
      |    }
      |
      |}
      |
      |""".stripMargin

  "[True Positive] The value stored in the field \"secret\" of class \"Main\"" should "not be leaked via " +
    "System.out.println()." taggedAs(ClassInitializer, ExplicitFlows) in {
    assertIsInsecure(TaintSpec(
      cpg.fieldAccess.code(".*secret.*"),
      cpg.call(".*println.*").argument
    ))
  }

}
