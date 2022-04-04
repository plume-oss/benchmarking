package com.github.plume.oss
package ifspec.simple

import ifspec.{ExplicitFlows, Simple}
import textfixtures.JimpleDataflowFixture

class DirectAssignmentSecure extends JimpleDataflowFixture {

  behavior of
    """The method 'leakyMethod' returns its parameter.
      |""".stripMargin

  override val code: String =
    """class DirectAssignment {
      |
      |    public static void main (String [] args) {
      |        leakyMethod(randInt());
      |    }
      |
      |	public static int leakyMethod(int high) {
      |		return 0;
      |	}
      |
      |    /** Helper methot to obtain a random integer */
      |    static int randInt() {
      |        return 42;
      |    }
      |}
      |
      |""".stripMargin

  "[Secure] The parameter of 'leakyMethod'" should "not flow to its return value" taggedAs (Simple, ExplicitFlows) in {
    assertIsSecure(specLeakyMethodInputToReturn)
  }

}
