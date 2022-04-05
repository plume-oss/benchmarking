package com.github.plume.oss
package ifspec.array

import textfixtures.JimpleDataflowFixture

import ifspec.IFSpecTags._
import io.shiftleft.semanticcpg.language._

class ArrayIndexExceptionSecure extends JimpleDataflowFixture {

  behavior of
    """The program loops over an array that has the length of the secret value.
      |If an array index exception occurs, the program is terminated.
      |This does not leak the secret value under our security model, but the timing
      |of the program might be dependent on the secret value.
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
      |                System.exit(0);
      |            }
      |        }
      |    }
      |}
      |
      |""".stripMargin

  "[Secure] The value stored in the field \"secret\" of class \"Main\"" should "not be leaked via System.out.println()" taggedAs (Arrays, ImplicitFlows, Exceptions) in {
    assertIsSecure(
      TaintSpec(
        cpg.fieldAccess.code("Main.secret"),
        cpg.method("main").call(".*println.*").argument(1),
      )
    )
  }

}
