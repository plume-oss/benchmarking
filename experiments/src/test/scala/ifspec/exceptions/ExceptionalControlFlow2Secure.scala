package com.github.plume.oss
package ifspec.exceptions

import ifspec.IFSpecTags._
import textfixtures.JimpleDataflowFixture

class ExceptionalControlFlow2Secure extends JimpleDataflowFixture {

  behavior of
    """This program calls the method 'foo' with a random boolean value.
      |No matter what the input was, an exception is thrown and immediately caught.
      |When catching the exception, the return value is set to true.
      |This value is then returned by the method.
      |""".stripMargin

  override val code: String =
    """class program {
      |
      |    private static class T extends Exception {}
      |
      |    /** Main test method parameter is the secret, return value is public */
      |    static boolean foo(boolean h) {
      |      boolean x = false;
      |      try {
      |        if (h) {throw new T();}
      |        else {throw new T();}
      |      } catch (T t){
      |        x = true;
      |      }
      |      return x;
      |    }
      |
      |    public static void main (String [] args) {
      |        foo(randBool());
      |    }
      |
      |    /** Helper method to obtain a random boolean */
      |    static boolean randBool() {
      |        return true;
      |    }
      |    /** Helper method to obtain a random integer */
      |    static int randInt() {
      |        return 42;
      |    }
      |
      |}
      |
      |""".stripMargin

  "[Secure] The desired security requirement is that an attacker that can only observe the return value " +
    "of the method 'foo' cannot determine the random boolean value. This means that no information" should
    "flow from the method's input to its return value" taggedAs(Exceptions, ImplicitFlows) in {
    assertIsSecure(specFooInputLeakedToReturn)
  }

}
