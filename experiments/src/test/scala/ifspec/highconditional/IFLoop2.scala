package com.github.plume.oss
package ifspec.highconditional

import ifspec.IFSpecTags._
import textfixtures.JimpleDataflowFixture

class IFLoop2 extends JimpleDataflowFixture {

  behavior of
    """In the program, a loop is executed and the local variables x and y are incremented (starting from zero) until
      |the value of the local variable y reaches 10. In each iteration, the method print() is called and assigns the
      |current value of x to the field low. When the value of y reaches five, x is set to the value of the field high.
      |""".stripMargin

  override val code: String =
    """class IFLoop {
      |    public int low;
      |    private int high;
      |
      |    public static void main(String[] args) {
      |        IFLoop ifl = new IFLoop();
      |        ifl.insecure_ifl();
      |    }
      |
      |    public void insecure_ifl() {
      |        int x = 0;
      |        int y = 0;
      |        while (y < 10) {
      |            print(x);
      |            if (y == 5) {
      |                x = high;
      |            }
      |            x++;
      |            y++;
      |        }
      |    }
      |
      |    public void print(int x) {
      |        low = x;
      |    }
      |}
      |
      |""".stripMargin

  "[Insecure] There" should "not be any flow of information from the field high to the field low." taggedAs(HighConditional, ExplicitFlows) in {
    assertIsInsecure(specFieldHighLow)
  }

}
