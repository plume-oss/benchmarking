package com.github.plume.oss
package ifspec.simple

import ifspec.IFSpecTags._
import textfixtures.JimpleDataflowFixture

class IFMethodContract extends JimpleDataflowFixture {

  behavior of
    """The program branches on whether the value of the field high is positive and either a) calls the method
      |n5() on the value of the field high and stores the result in the field low, or b) negates the value of the
      |field high and then calls the method n5() on the value of the field high added to the value of the field low
      | and stores the result in the field low.
      |""".stripMargin

  override val code: String =
    """class IFMethodContract {
      |    public static int low;
      |    private static int high;
      |
      |    public static void main(String[] args) {
      |        IFMethodContract ifm = new IFMethodContract();
      |        ifm.secure_if_high_n5_n1();
      |    }
      |
      |    void secure_if_high_n5_n1() {
      |        if (high > 0) {
      |            low = n5(high);
      |        } else {
      |            high = -high;
      |            low = n5(high + low);
      |        }
      |
      |    }
      |
      |    int n5(int x) {
      |        high = 2 * x;
      |        return 15;
      |    }
      |
      |}
      |
      |""".stripMargin

  "[Secure] There" should "not be any flow of information from the field high to the field low" taggedAs (Simple, ExplicitFlows) in {
    assertIsSecure(specFieldHighLow)
  }

}
