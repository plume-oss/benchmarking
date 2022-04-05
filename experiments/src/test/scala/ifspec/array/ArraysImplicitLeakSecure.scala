package com.github.plume.oss
package ifspec.array

import ifspec.IFSpecTags._
import textfixtures.JimpleDataflowFixture
import io.shiftleft.semanticcpg.language._

class ArraysImplicitLeakSecure extends JimpleDataflowFixture {

  behavior of
    """The program saves the secret value at position 0 of an array.
      |If the value at position 0 has a certain value, a message is printed.
      |Otherwise, the same message is printed.
      |This does not leak whether the secret value has the tested value.
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
      |            System.out.println("checked");
      |        } else {
      |            System.out.println("checked");
      |        }
      |
      |    }
      |
      |}
      |
      |""".stripMargin

  "[Secure] The value stored in the field \"secret\" of class \"Main\"" should "not be leaked via System.out.println()" taggedAs (Arrays, ImplicitFlows) in {
    assertIsSecure(
      TaintSpec(
        cpg.fieldAccess.code("Main.secret"),
        cpg.method("main").call(".*println.*").argument(1),
      )
    )
  }

}
