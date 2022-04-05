package com.github.plume.oss
package ifspec.library

import ifspec.IFSpecTags._
import textfixtures.JimpleDataflowFixture

class ImplicitListSizeNoLeak extends JimpleDataflowFixture {

  behavior of
    """The program does not leak secret information via the size of an ArrayList.
      |Even though the return value is set to the secret-dependent list size, it is updated to a fixed value afterwards.
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
      |	public static int listSizeLeak(int h) {
      |		ArrayList<Integer> list = new ArrayList<Integer>();
      |
      |		int r = 0;
      |
      |		for (int i = 0; i < h; i++) {
      |			list.add(42);
      |		}
      |
      |		if (list.size() < 10) {
      |			r = 1;
      |		}
      |
      |		r = 0;
      |
      |		return r;
      |	}
      |}
      |
      |""".stripMargin

  "[Secure] The input provided to the method listSizeLeak(int h)" should "not be leaked by the return value of " +
    "this method. That is, the input provided to the method listSizeLeak(int h) is the confidential information and " +
    "the return value of this method is visible to the attacker" taggedAs(Library, ImplicitFlows) in {
    assertIsSecure(specListSizeInputLeakedToReturn)
  }

}