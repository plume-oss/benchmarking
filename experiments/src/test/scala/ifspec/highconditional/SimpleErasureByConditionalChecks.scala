package com.github.plume.oss
package ifspec.highconditional

import ifspec.IFSpecTags._
import textfixtures.JimpleDataflowFixture

import io.shiftleft.semanticcpg.language._

class SimpleErasureByConditionalChecks extends JimpleDataflowFixture {

  behavior of
    """This example provides a program where a function works on a secret input,
      |checks whether the given input is less or higher than 0 and sets the eventual
      |return value to an intermediate value (that differs between the comparison results).
      |
      |Eventually, however, the return value is set to a constant value, no matter what
      |the comparison resulted in before.
      |""".stripMargin

  override val code: String =
    """
      |class SimpleErasureByConditionalChecks {
      |
      |    public static void main(String args[]) {
      |        computeSecretly(12);
      |    }
      |
      |    // compare the secret input to 0, set the return value to some
      |    // intermediate value, but set the return value to 5 eventually
      |    private static int computeSecretly(int h) {
      |        int a = 42;
      |
      |        if (h > 0) {
      |            a = 5;
      |        } else {
      |            a = 3;
      |        }
      |
      |        if (h <= 0) {
      |            a = 5;
      |        }
      |
      |        return a;
      |    }
      |}
      |
      |""".stripMargin

  "[Secure] The requirement is that the input provided to the method computeSecretely(int)" should "not be" +
    " leaked by the return value of the method. That is, the input to the method computeSecretly(int) is the" +
    " confidential information and the return value of the method is visible " +
    "to an attacker." taggedAs(HighConditional, ImplicitFlows) in {
    assertIsSecure(
      TaintSpec(
        cpg.method("main").call(".*computeSecretly.*").argument,
        cpg.method("computeSecretly").methodReturn
      )
    )
  }

}
