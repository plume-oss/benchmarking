package com.github.plume.oss
package ifspec.casting

import ifspec.IFSpecTags._
import textfixtures.JimpleDataflowFixture

import io.shiftleft.semanticcpg.language._

class LostInCast extends JimpleDataflowFixture {

  behavior of
    """The class lostInCast takes in input an integer value, performs some meaningless mathematical computation and
      |prints an integer.
      |""".stripMargin

  override val code: String =
    """class lostInCast {
      |
      |	public static int getRandomInt() {
      |		return 42;
      |	}
      |
      |	public static int doIt(int h) {
      |		// Assign the high variable h to the most significant half of the long variable
      |		// x
      |		long x = h * 256 * 256 * 256 * 256; // 4bytes to the left
      |
      |		// Fill the least-significant part of x with random garbage
      |		x += (getRandomInt());
      |
      |		// Assign x to the low variable l
      |		// Here is where the "magic" happens
      |		// The casting form long to int drops the four most significant bytes, which
      |		// contain the secret
      |		int l = (int) x;
      |
      |		return l;
      |	}
      |
      |	public static void main(String[] args) {
      |		// Assign first input parameter to the high variable
      |		int h = getRandomInt();
      |
      |		// Do very important math
      |		doIt(h);
      |	}
      |}
      |
      |""".stripMargin

  "[True Negative] The number passed to the method doIt()" should "not be leaked to its return value. " +
    "(basically, the output should be independent of the input)" taggedAs(Casting, ExplicitFlows) in {
    assertIsSecure(TaintSpec(
      cpg.method("main").call(".*doIt.*").argument(1),
      cpg.method("doIt").methodReturn
    ))
  }

}
