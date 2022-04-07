package com.github.plume.oss
package ifspec.highconditional

import ifspec.IFSpecTags._
import textfixtures.JimpleDataflowFixture
import io.shiftleft.semanticcpg.language._

class HighConditionalIncrementalLeakInsecure extends JimpleDataflowFixture {

  behavior of
    """The method f is provided with two inputs h and l.
      |Via a loop the value of h is added to the value of l if h is positive, otherwise, l remains unchanged.
      |Finally, the value of l is returned.
      |""".stripMargin

  override val code: String =
    """class Eg2 {
      |	public static void main(String args[]) {
      |		int h = 5;
      |		int l = 1;
      |		f(h, l);
      |		// System.out.println(f(h, l));
      |	}
      |
      |	public static int f(int h, int l) {
      |		while (h > 0) {
      |			h--;
      |			l++;
      |		}
      |		return l;
      |	}
      |}
      |
      |""".stripMargin

  "[True Positive] The method f in the class Eg2" should "not leak any information from its parameter h to the return " +
    "value of f. That is, the value h provided as input to the method f is confidential and the return value of f " +
    "is assumed to be observable by the attacker. Moreover, it assumed that the input l of the method m is known to " +
    "the attacker, i.e., provided by the attacker or visible to the " +
    "attacker when provided as input to the method" taggedAs (HighConditional, ImplicitFlows) in {
    assertIsInsecure(
      TaintSpec(
        cpg.call(".*f.*").argument,
        cpg.method("f").methodReturn,
      )
    )
  }

}
