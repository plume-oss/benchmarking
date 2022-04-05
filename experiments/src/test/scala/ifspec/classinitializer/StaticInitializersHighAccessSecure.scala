package com.github.plume.oss
package ifspec.classinitializer

import ifspec.IFSpecTags._
import textfixtures.JimpleDataflowFixture

import io.shiftleft.semanticcpg.language._

class StaticInitializersHighAccessSecure extends JimpleDataflowFixture {

  behavior of
    """The program stores the secret value in class A when it is initialized and
      |prints a String that is not dependent on the secret value. Hence, no
      |information about the secret value is leaked.
      |""".stripMargin

  override val code: String =
    """class Main {
      |
      |    private static String secret = "secret";
      |
      |    static class A {
      |        static String stored;
      |
      |        static {
      |            stored = secret;
      |            System.out.println("initialized");
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

  "[Secure] The value stored in the field \"secret\" of class \"Main\"" should "not be leaked via " +
    "System.out.println()." taggedAs(ClassInitializer, ExplicitFlows) in {
    assertIsSecure(TaintSpec(
      cpg.fieldAccess.code(".*secret.*"),
      cpg.call(".*println.*").argument
    ))
  }

}
