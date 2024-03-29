package com.github.plume.oss
package ifspec.exceptions

import ifspec.IFSpecTags._
import textfixtures.JimpleDataflowFixture
import io.shiftleft.semanticcpg.language._

class ExceptionalControlFlow1Secure extends JimpleDataflowFixture {

  behavior of
    """This program calls the method 'foo' with a random boolean value.
      |If the random value is true, an exception is thrown and immediately caught.
      |Finally, foo returns true.
      |""".stripMargin

  override val code: String =
    """class program {
      |
      |  private static class T extends Exception {
      |  }
      |
      |  /** Main test method parameter is the secret, return value is public */
      |  static boolean foo(boolean h) {
      |    boolean x = false;
      |    try {
      |      if (h) {
      |        throw new T();
      |      }
      |    } catch (T t) {
      |      // Nothing happening here
      |    }
      |    x = true;
      |    return x;
      |  }
      |
      |  public static void main(String[] args) {
      |    foo(randBool());
      |  }
      |
      |  /** Helper method to obtain a random boolean */
      |  static boolean randBool() {
      |    return true;
      |  }
      |
      |  /** Helper method to obtain a random integer */
      |  static int randInt() {
      |    return 42;
      |  }
      |
      |}
      |
      |""".stripMargin

  "[True Negative] The desired security requirement is that an attacker that can only observe the return value " +
    "of the method 'foo' cannot determine the random boolean value. This means that no information" should
    "flow from the method's input to its return value" taggedAs (Exceptions, ImplicitFlows) in {
    assertIsSecure(
      TaintSpec(
        cpg.call(".*foo.*").argument(1),
        cpg.method("foo").methodReturn
      )
    )
  }

}
