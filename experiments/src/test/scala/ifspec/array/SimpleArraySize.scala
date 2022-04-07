package com.github.plume.oss
package ifspec.array

import ifspec.IFSpecTags._
import textfixtures.JimpleDataflowFixture
import io.shiftleft.semanticcpg.language._

class SimpleArraySize extends JimpleDataflowFixture {

  behavior of
    """The method arraySizeLeak accepts a number as input, creates an array that has the size of the given number and
      |returns the size of the array.
      |""".stripMargin

  override val code: String =
    """
      |class simpleArraySize {
      |
      |	public static void main(String[] args) {
      |		int value = 5;
      |		simpleArraySize.arraySizeLeak(value);
      |	}
      |
      |	/**
      |	 * Returns the number that was given, by passing
      |	 * it trough an array size.
      |	 *
      |	 * @param h secret value
      |	 * @return value given
      |	 */
      |	public static int arraySizeLeak(int h) {
      |		int[] array = new int[h];
      |		return array.length;
      |	}
      |}
      |
      |""".stripMargin

  "[True Positive] The program" should "not leak any information provided as input to the method" +
    "arraySizeLeak(int h) to the return value of this method which is assumed to be " +
    "observable by the attacker." taggedAs (Arrays, ExplicitFlows) in {
    assertIsInsecure(TaintSpec(
      cpg.call(".*arraySizeLeak.*").argument(1),
      cpg.method("arraySizeLeak").ast.isReturn
    ))
  }

}
