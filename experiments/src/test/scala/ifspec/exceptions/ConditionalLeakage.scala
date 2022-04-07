package com.github.plume.oss
package ifspec.exceptions

import ifspec.IFSpecTags._
import textfixtures.JimpleDataflowFixture
import io.shiftleft.semanticcpg.language._

class ConditionalLeakage extends JimpleDataflowFixture {

  behavior of
    """The given method takes two parameters l and h.
      |It tries to divide l by h. If this fails, i.e. if h is 0, then an error message is printed to the console.
      |""".stripMargin

  override val code: String =
    """import java.lang.ArithmeticException;
      |
      |class DivisionByZero {
      |	public static int divide(int l, int h) {
      |		int z = 0;
      |		try {
      |			z = l / h;
      |		} catch (ArithmeticException e) {
      |			System.out.println(h + " is not defined");
      |		}
      |		return z;
      |	}
      |
      |	public static void main(String[] args) {
      |		divide(randInt(), randInt());
      |	}
      |
      |	/** Helper method to obtain a random integer */
      |	static int randInt() {
      |		return 42;
      |	}
      |}
      |
      |""".stripMargin

  "[True Positive] The second parameter of the method 'divide'" should
    "flow to public output (i.e. System.out.println)" taggedAs (Exceptions, ImplicitFlows) in {
    assertIsInsecure(
      TaintSpec(
        cpg.call(".*divide.*").argument(2),
        cpg.method(".*divide.*").call(".*println.*").argument(1),
      )
    )
  }

}
