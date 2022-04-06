package com.github.plume.oss
package ifspec.highconditional

import ifspec.IFSpecTags._
import textfixtures.JimpleDataflowFixture
import io.shiftleft.semanticcpg.language._

class IFLoop1 extends JimpleDataflowFixture {

  behavior of
    """In the program, a loop is executed and the local variables x and y are incremented (starting from zero) until
      |the value of the local variable y reaches 10. In each iteration, x is assigned to the local variable 'low'.
      |When the value of y reaches five, x is set to the value of the parameter and the value of y is set to nine.
      |At the end, the local variable low is returned.
      |""".stripMargin

  override val code: String =
    """class IFLoop {
      |
      |	public static void main(String[] args) {
      |		IFLoop ifl = new IFLoop();
      |		ifl.secure_ifl(17);
      |	}
      |
      |	public int secure_ifl(int high) {
      |		int x = 0;
      |		int y = 0;
      |		int low = 23;
      |		// @ loop_invariant 0 <= y && y <= 10;
      |		// @ determines low, y, (y < 10 ? x : 0) \by \itself;
      |		// @ assignable low;
      |		// @ decreases 10 - y;
      |		while (y < 10) {
      |			low = x;
      |			if (y == 5) {
      |				x = high;
      |				y = 9;
      |			}
      |			x++;
      |			y++;
      |		}
      |		return low;
      |	}
      |}
      |
      |""".stripMargin

  "[True Negative] There" should "not be any flow of information from the parameter of 'secure_ifl' to its" +
    " return value" taggedAs (HighConditional, ExplicitFlows) in {
    assertIsSecure(
      TaintSpec(
        cpg.method("secure_ifl").call(".*secure_ifl.*").argument(1),
        cpg.method("secure_ifl").methodReturn,
      )
    )
  }

}
