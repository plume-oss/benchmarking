package com.github.plume.oss
package ifspec

import textfixtures.JimpleDataflowFixture

class ArrayIndexExceptionInsecure extends JimpleDataflowFixture {

  behavior of
    """The program loops over an array that has the length of the secret value.
      |If an array index exception occurs, the value of the accessed index is printed.
      |This leaks the secret value.
      |""".stripMargin

  override val code: String =
    """class Main {
      |
      |    static int secret=42;
      |
      |    public static void main(String[] args) {
      |        int[] arr = new int[secret];
      |
      |        for (int i=0; i<Integer.MAX_VALUE; i++) {
      |            try {
      |                int j=arr[i];
      |            } catch (Exception e) {
      |                System.out.println(i);
      |                System.exit(0);
      |            }
      |        }
      |    }
      |}
      |
      |""".stripMargin

  "[Insecure] The value stored in the field \"secret\" of class \"Main\"" should "be leaked via System.out.println()" in {
    assertIsInsecure(specMainSecretLeakedToPrintln)
  }

}
