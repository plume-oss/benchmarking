package com.github.plume.oss
package ifspec.array

import ifspec.{Arrays, ExplicitFlows}
import textfixtures.JimpleDataflowFixture

class ArraySizeStrongUpdate extends JimpleDataflowFixture {

  behavior of
    """The program creates an array with the length of the secret value.
      |This array is then recreated with a different length.
      |Finally, the length is printed, which does not leak the secret value.
      |""".stripMargin

  override val code: String =
    """class Main {
      |
      |    static int secret=42;
      |
      |    public static void main(String[] args) {
      |        int[] a = new int[secret];
      |        a = new int[5];
      |        System.out.println(a.length);
      |    }
      |}
      |
      |""".stripMargin

  "[Secure] The value stored in the field \"secret\" of class \"Main\"" should "not be leaked via System.out.println()" taggedAs (Arrays, ExplicitFlows) in {
    assertIsSecure(specMainSecretLeakedToPrintln)
  }

}
