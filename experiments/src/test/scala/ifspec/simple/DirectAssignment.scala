package com.github.plume.oss
package ifspec.simple

import ifspec.IFSpecTags._
import textfixtures.JimpleDataflowFixture
import io.shiftleft.semanticcpg.language._

class DirectAssignment extends JimpleDataflowFixture {

  behavior of
    """The method 'leakyMethod' returns its parameter.
      |""".stripMargin

  override val code: String =
    """class DirectAssignment {
      |
      |    public static void main(String[] args) {
      |        leakyMethod(randInt());
      |    }
      |
      |    public static int leakyMethod(int high) {
      |        return high;
      |    }
      |
      |    /** Helper method to obtain a random integer */
      |    static int randInt() {
      |        return 42;
      |    }
      |}
      |
      |""".stripMargin

  "[True Positive] The parameter of 'leakyMethod'" should "flow to its return value" taggedAs (Simple, ExplicitFlows) in {
    assertIsInsecure(
      TaintSpec(
        cpg.call(".*leakyMethod.*").argument(1),
        cpg.method("leakyMethod").ast.isReturn
      )
    )
  }

}
