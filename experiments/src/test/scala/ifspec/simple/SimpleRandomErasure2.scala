package com.github.plume.oss
package ifspec.simple

import ifspec.IFSpecTags._
import textfixtures.JimpleDataflowFixture

import io.shiftleft.semanticcpg.language._

class SimpleRandomErasure2 extends JimpleDataflowFixture {

  behavior of
    """The program consists of a single class simpleRandomErasure with a secret field secret. The program first assigns
      |the secret to an output variable. Afterwards it modifies the output variable by adding the difference between a
      |random number and the secret itself to it. Then the resulting value is printed to the standard output.
      |""".stripMargin

  override val code: String =
    """
      |import java.util.Random;
      |
      |class simpleRandomErasure {
      |	private static int secret = 42;
      |
      |	public static void main(String[] args) {
      |		int output = secret;
      |
      |		Random random = new Random();
      |		output += random.nextInt(Integer.MAX_VALUE) - secret;
      |
      |		System.out.println(Integer.toString(output));
      |	}
      |}
      |
      |""".stripMargin

  "[True Negative] The field secret in the class simpleRandomErasure" should "not be printed to standard output. That is," +
    " the field secret is the confidential information of the program and the attacker is able to observe the " +
    "standard output." taggedAs(Simple, ImplicitFlows) in {
    assertIsSecure(
      TaintSpec(
        cpg.fieldAccess.code(".*secret.*"),
        cpg.call("println").argument
      )
    )
  }

}
