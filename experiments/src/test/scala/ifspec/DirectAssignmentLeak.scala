package com.github.plume.oss
package ifspec

import textfixtures.JimpleDataflowFixture

class DirectAssignmentLeak extends JimpleDataflowFixture {

  behavior of
    """The method 'f' copies the value of its first parameter into its second parameter and returns the second parameter.
      |""".stripMargin

  override val code: String =
    """class Eg1 {
      |	public static void main(String args[]){
      |		f(randInt(), randInt());
      |	}
      |
      |	public static int f(int h, int l)
      |	{
      |		l = h;
      |		return l;
      |	}
      |
      |    /** Helper method to obtain a random integer */
      |    static int randInt() {
      |        return 42;
      |    }
      |
      |}
      |
      |""".stripMargin

  "[Insecure] The first parameter of method 'f'" should "flow into the return value of method 'f'." in {
    assertIsInsecure(specFInput1LeakedToReturn)
  }

}
