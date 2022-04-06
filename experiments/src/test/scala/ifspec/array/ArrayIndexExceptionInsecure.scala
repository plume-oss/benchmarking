package com.github.plume.oss
package ifspec.array

import ifspec.IFSpecTags._
import textfixtures.JimpleDataflowFixture
import io.shiftleft.semanticcpg.language._

class ArrayIndexExceptionInsecure extends JimpleDataflowFixture {

  behavior of
    """The program loops over an array that has the length of the secret value.
      |If an array index exception occurs, the value of the accessed index is printed.
      |This leaks the secret value.
      |""".stripMargin

  override val code: String =
    """class Main {
      |
      |    static int secret = 42;
      |
      |    public static void main(String[] args) {
      |        int[] arr = new int[secret];
      |
      |        for (int i = 0; i < Integer.MAX_VALUE; i++) {
      |            try {
      |                int j = arr[i];
      |            } catch (Exception e) {
      |                System.out.println(i);
      |                System.exit(0);
      |            }
      |        }
      |    }
      |}
      |
      |""".stripMargin

  "[True Positive] The value stored in the field \"secret\" of class \"Main\"" should "be leaked via " +
    "System.out.println()" taggedAs (Arrays, ImplicitFlows, Exceptions) in {
    assertIsInsecure(
      TaintSpec(
        cpg.fieldAccess.code("Main.secret"),
        cpg.method("main").call(".*println.*").argument(1),
      )
    )
  }

}
