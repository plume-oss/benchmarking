package com.github.plume.oss
package ifspec.exceptions

import ifspec.IFSpecTags._
import textfixtures.JimpleDataflowFixture
import io.shiftleft.semanticcpg.language._

class ExceptionHandling extends JimpleDataflowFixture {

  behavior of
    """The program reads a number from the console and either prints 0 or 2, depending on whether the number is negative.
      |""".stripMargin

  override val code: String =
    """class TryCatch {
      |    static int g(int a) throws Exception {
      |        if (a < 0)
      |            throw new Exception();
      |        return 1;
      |    }
      |
      |    static int f(int a) {
      |        int x;
      |        try {
      |            x = g(a);
      |            x++;
      |        } catch (Exception e) {
      |            x = 0;
      |        }
      |        return x;
      |    }
      |
      |    public static void main(String args[]) throws Exception {
      |        f(42);
      |    }
      |};
      |
      |""".stripMargin

  "[True Positive] The program" should "not leak any information about the value read from the console to System.out. " +
    "This means that no flow of information from the return value of readLine() to the parameter of println() is " +
    "allowed." taggedAs (Exceptions, ImplicitFlows) in {
    assertIsInsecure(
      TaintSpec(
        cpg.method("main").call(".*f.*").argument(1),
        cpg.method("f").methodReturn,
      )
    )
  }

}
