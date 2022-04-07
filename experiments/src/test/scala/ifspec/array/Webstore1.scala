package com.github.plume.oss
package ifspec.array

import ifspec.IFSpecTags._
import textfixtures.JimpleDataflowFixture

import io.shiftleft.semanticcpg.language._

class Webstore1 extends JimpleDataflowFixture {

  behavior of
    """The class Webstore offers possibilities to buy products and similar functionality.
      |Therefore, the user provides high and low input, but some sinks should only return low values, since their result
      |might be shown in the browser.
      |""".stripMargin

  override val code: String =
    """class Webstore {
      |   public int low;
      |   private int high;
      |
      |   private static int h;
      |   private static int l;
      |
      |   private int[] transaction;
      |
      |   public static void main(String[] args) {
      |
      |      Webstore w = new Webstore();
      |      w.buyProduct(l, h);
      |   }
      |
      |   /*
      |    * Customer buys the product and wants to pay with the given credit card number.
      |    * Afterwards, the store returns the bought product
      |    */
      |   public int buyProduct(int prod, int cc) {
      |      this.transaction = new int[2];
      |      this.transaction[0] = prod;
      |      this.transaction[1] = cc;
      |
      |      return this.transaction[0];
      |   }
      |}
      |
      |""".stripMargin

  "[True Negative] There" should "be no flow from high to low." taggedAs(Arrays, ExplicitFlows) in {
    assertIsSecure(TaintSpec(
      cpg.call(".*buyProduct.*").argument,
      cpg.method("buyProduct").ast.isReturn
    ))
  }

}
