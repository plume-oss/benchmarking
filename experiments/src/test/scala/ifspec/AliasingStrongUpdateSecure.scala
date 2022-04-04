package com.github.plume.oss
package ifspec

import textfixtures.JimpleDataflowFixture

class AliasingStrongUpdateSecure extends JimpleDataflowFixture {

  behavior of
    """The program prints a field from an object that contains the secret value
      |at some point of the execution, but is changed before the print operation happens.
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
      |    }
      |
      |    static int secret=42;
      |
      |    public static void main(String[] arg) {
      |        A a = new A(secret);
      |        A b = new A(5);
      |        A c = b;
      |
      |        b = a;
      |
      |        a.val = 2;
      |
      |        System.out.println(c.val);
      |    }
      |}
      |
      |""".stripMargin

  "[Secure] The value stored in the field \"secret\" of class \"Main\"" should "not be leaked via System.out.println()" in {
    assertIsSecure(specMainSecretLeakedToPrintln)
  }

}
