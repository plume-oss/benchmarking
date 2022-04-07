package com.github.plume.oss
package ifspec.array

import ifspec.IFSpecTags._
import textfixtures.JimpleDataflowFixture
import io.shiftleft.semanticcpg.language._

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

  "[True Negative] The value stored in the field \"secret\" of class \"Main\"" should "not be leaked " +
    "via System.out.println()" taggedAs (Arrays, ExplicitFlows) in {
    assertIsSecure(
      TaintSpec(
        cpg.fieldAccess.code("Main.secret"),
        cpg.call(".*println.*").argument(1),
      )
    )
  }

}
