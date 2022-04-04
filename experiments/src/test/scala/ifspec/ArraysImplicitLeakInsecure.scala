package com.github.plume.oss
package ifspec

import textfixtures.JimpleDataflowFixture

class ArraysImplicitLeakInsecure extends JimpleDataflowFixture {

  behavior of
    """The program saves the secret value at position 0 of an array.
      |If the value at position 0 has a certain value, a message is printed.
      |This leaks whether the secret value has the tested value.
      |""".stripMargin

  override val code: String =
    """class Main {
      |
      |    static private int secret = 42;
      |
      |    public static void main(String[] args) {
      |
      |        int[] arr = new int[5];
      |        arr[0] = secret;
      |
      |        if (arr[0] == 42) {
      |            System.out.println("Found");
      |        }
      |
      |    }
      |
      |}
      |
      |""".stripMargin

  "The value stored in the field \"secret\" of class \"Main\"" should "be leaked via System.out.println()" in {
    assertIsInsecure(specMainSecretLeakedToPrintln)
  }

}
