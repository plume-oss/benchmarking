package com.github.plume.oss
package ifspec.classinitializer

import ifspec.IFSpecTags._
import textfixtures.JimpleDataflowFixture

import io.shiftleft.semanticcpg.language._

class StaticInitializersNotCalled extends JimpleDataflowFixture {

  behavior of
    """When class A is initialized, the secret value is leaked by printing it.
      |However, class A is never loaded, and thus the secret value is not leaked.
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
      |    }
      |
      |    public static void main(String[] args) {
      |        System.out.println("nothing here.");
      |    }
      |}
      |
      |""".stripMargin

  "[True Negative] The value stored in the field \"secret\" of class \"Main\"" should "not be leaked via " +
    "System.out.println()." taggedAs(ClassInitializer, ExplicitFlows) in {
    assertIsSecure(TaintSpec(
      cpg.fieldAccess.code(".*secret.*"),
      cpg.call(".*println.*").argument
    ))
  }

}
