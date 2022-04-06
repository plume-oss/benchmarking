package com.github.plume.oss
package ifspec.simple

import ifspec.IFSpecTags._
import textfixtures.JimpleDataflowFixture
import io.shiftleft.semanticcpg.language._

class IFMethodContract2 extends JimpleDataflowFixture {

  behavior of
    """The program branches on whether the value of the high parameter is positive and either a) calls the method
      |n5() on the value of high, or b) assigns the value 7 to the local variable 'low'. Afterwards, 'low' is
      |overwritten with the result of method 'n5' and returned.
      |""".stripMargin

  override val code: String =
    """class IFMethodContract {
      |    public int low;
      |    private int high;
      |
      |    public static void main(String[] args) {
      |        IFMethodContract ifm = new IFMethodContract();
      |        ifm.insecure_if_high_n1(42);
      |    }
      |
      |    int insecure_if_high_n1(int high) {
      |        int low;
      |        if (high > 0) {
      |            low = n5(high);
      |        } else {
      |            low = 7;
      |        }
      |        low = n1(high);
      |        return low;
      |    }
      |
      |    int n1(int x) {
      |        return 27;
      |    }
      |
      |    int n5(int x) {
      |        return 15;
      |    }
      |
      |}
      |
      |""".stripMargin

  "[True Negative] There" should "not be any flow of information from the parameter to the return value" taggedAs (Simple, ExplicitFlows) in {
    assertIsSecure(
      TaintSpec(
        cpg.method("main").call(".*insecure_if_high_n1.*").argument(1),
        cpg.method("insecure_if_high_n1").methodReturn,
      )
    )
  }

}
