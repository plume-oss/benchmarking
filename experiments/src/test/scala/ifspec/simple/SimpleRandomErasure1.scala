package com.github.plume.oss
package ifspec.simple

import ifspec.IFSpecTags._
import textfixtures.JimpleDataflowFixture

import io.shiftleft.semanticcpg.language._

class SimpleRandomErasure1 extends JimpleDataflowFixture {

  behavior of
    """The program consists of a single class simpleRandomErasure with a secret field secret. The program first
      |assigns the secret to an output variable. Afterwards it generates a random boolean and then depending on the
      |boolean variable assigns a random value to the output variable where the range of the random value depends
      |on both the random boolean variable and the secret. Then the resulting value is printed to the standard output.
      |""".stripMargin

  override val code: String =
    """import java.util.Random;
      |
      |class simpleRandomErasure {
      |	private static int secret = 42;
      |
      |	public static void main(String[] args) {
      |		int output = secret;
      |
      |		Random random = new Random();
      |		if (random.nextBoolean()) {
      |			output += random.nextInt(Integer.MAX_VALUE - secret);
      |		} else {
      |			output -= random.nextInt(-(Integer.MIN_VALUE + secret));
      |		}
      |
      |		System.out.println(Integer.toString(output));
      |	}
      |}
      |
      |""".stripMargin

  "[True Positive] The field secret in the class simpleRandomErasure" should "not be printed to standard output. " +
    "That is, the field secret is the confidential information of the program and the attacker is able to " +
    "observe the standard output." taggedAs(Simple, ImplicitFlows) in {
    assertIsInsecure(
      TaintSpec(
        cpg.fieldAccess.code(".*secret.*"),
        cpg.call("println").argument
      )
    )
  }

}
