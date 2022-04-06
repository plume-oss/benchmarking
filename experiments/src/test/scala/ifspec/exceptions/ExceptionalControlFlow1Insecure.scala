package com.github.plume.oss
package ifspec.exceptions

import ifspec.IFSpecTags._
import textfixtures.JimpleDataflowFixture
import io.shiftleft.semanticcpg.language._

class ExceptionalControlFlow1Insecure extends JimpleDataflowFixture {

  behavior of
    """This program creates a random boolean value and passes it to the method foo().
      |Depending on this value, an exception is thrown, which determines whether true or false is returned by foo().
      |""".stripMargin

  override val code: String =
    """class program {
      |
      |  private static class T extends Exception {
      |  }
      |
      |  /** Main test method parameter is the secret, return value is public */
      |  static boolean foo(boolean h) {
      |    try {
      |      if (h) {
      |        throw new T();
      |      }
      |    } catch (T t) {
      |      return true;
      |    }
      |    return false;
      |  }
      |
      |  public static void main(String[] args) {
      |    foo(randBool());
      |  }
      |
      |  /** Helper method to obtain a random boolean */
      |  static boolean randBool() {
      |    return System.currentTimeMillis() % 2 == 0;
      |  }
      |
      |  /** Helper method to obtain a random integer */
      |  static int randInt() {
      |    return (int) System.currentTimeMillis();
      |  }
      |
      |}
      |
      |""".stripMargin

  "[Insecure] No information about the value of the parameter of the method foo()" should
    "flow to the return value of foo()" taggedAs (Exceptions, ImplicitFlows) in {
    assertIsInsecure(
      TaintSpec(
        cpg.method("main").call(".*foo.*").argument(1),
        cpg.method("foo").methodReturn
      )
    )
  }

}
