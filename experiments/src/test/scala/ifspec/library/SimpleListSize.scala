package com.github.plume.oss
package ifspec.library

import ifspec.IFSpecTags._
import textfixtures.JimpleDataflowFixture
import io.shiftleft.semanticcpg.language._

class SimpleListSize extends JimpleDataflowFixture {

  behavior of
    """The method listSizeLeak accepts a secret number as input, creates a list and adds #secret elements to it.
      |It returns the size of the list.
      |""".stripMargin

  override val code: String =
    """import java.util.ArrayList;
      |
      |class simpleListSize {
      |
      |	public static void main(String[] args) {
      |		int value = 5;
      |		System.out.println("Running simpleListSize");
      |		System.out.println("Secret value:   " + value);
      |		System.out.println("Returned value: " + simpleListSize.listSizeLeak(value));
      |	}
      |
      |	/**
      |	 * Returns the number that was given, by passing
      |	 * adding elements to a list and returning its size.
      |	 *
      |	 * @param h secret value
      |	 * @return value given
      |	 */
      |	public static int listSizeLeak(int h) {
      |		ArrayList<Integer> list = new ArrayList<Integer>();
      |
      |		for (int i = 0; i < h; i++) {
      |			list.add(42);
      |		}
      |
      |		return list.size();
      |	}
      |}
      |
      |""".stripMargin

  "[Insecure] The input provided to the method listSizeLeak(int h)" should "not be leaked by the return value of this" +
    " method. That is, the input provided to the method listSizeLeak(int h) is the confidential information and the " +
    "return value of this method is visible to the attacker." taggedAs(Library, ImplicitFlows) in {
    assertIsInsecure(TaintSpec(
      cpg.method("main").call(".*listSizeLeak.*").argument(1),
      cpg.method("listSizeLeak").methodReturn
    ))
  }

}
