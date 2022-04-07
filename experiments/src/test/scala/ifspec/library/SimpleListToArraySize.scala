package com.github.plume.oss
package ifspec.library

import ifspec.IFSpecTags._
import textfixtures.JimpleDataflowFixture

import io.shiftleft.semanticcpg.language._

class SimpleListToArraySize extends JimpleDataflowFixture {

  behavior of
    """The method listSizeLeak accepts a secret number as input, creates a list and adds #secret elements to it.
      |Afterwards it converts the array list to an array of the same length and returns the size of this array.
      |""".stripMargin

  override val code: String =
    """import java.util.ArrayList;
      |
      |class simpleListToArraySize {
      |	public static void main(String[] args) {
      |		int value = 5;
      |		System.out.println("Running simpleListToArraySize");
      |		System.out.println("Secret value:   " + value);
      |		System.out.println("Returned value: " + simpleListToArraySize.listArraySizeLeak(value));
      |	}
      |
      |	/**
      |	 * Returns the number that was given, by passing
      |	 * adding elements to a list, converting to an array
      |	 * and returning its size.
      |	 *
      |	 * @param h secret value
      |	 * @return value given
      |	 */
      |	public static int listArraySizeLeak(int h) {
      |		ArrayList<Integer> list = new ArrayList<Integer>();
      |
      |		for (int i = 0; i < h; i++) {
      |			list.add(42);
      |		}
      |
      |		Object[] array = list.toArray();
      |
      |		return array.length;
      |	}
      |}
      |
      |""".stripMargin

  "[True Positive] The input provided to the method listSizeLeak(int h)" should "not be leaked by the return value of " +
    "this method. That is, the input provided to the method listSizeLeak(int h) is the confidential information and " +
    "the return value of this method is visible to the attacker." taggedAs(Library, ImplicitFlows) in {
    assertIsInsecure(TaintSpec(
      cpg.call(".*listArraySizeLeak.*").argument,
      cpg.method("listArraySizeLeak").methodReturn
    ))
  }

}
