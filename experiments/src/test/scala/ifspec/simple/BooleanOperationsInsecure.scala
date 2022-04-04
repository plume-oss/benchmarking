package com.github.plume.oss
package ifspec.simple

import ifspec.IFSpecTags._
import textfixtures.JimpleDataflowFixture

class BooleanOperationsInsecure extends JimpleDataflowFixture {

  behavior of
    """This method does some computation on that value and returns that value.
      |The computation yields 'true', if the secret is 'true'.
      |""".stripMargin

  override val code: String =
    """class BooleanOperations {
      |	public static boolean leakyMethod(boolean high) {
      |		boolean ret;
      |		ret = (high && true);
      |		return ret;
      |	}
      |
      |	public static void main(String[] args){
      |	    BooleanOperations.leakyMethod(false);
      |	}
      |}
      |
      |""".stripMargin

  "There " should "be any flow from the parameter to the return value of the method" taggedAs (Simple, ExplicitFlows) in {
    assertIsInsecure(specLeakyMethodInputToReturn)
  }

}
