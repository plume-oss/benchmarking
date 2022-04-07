package com.github.plume.oss
package ifspec.library

import ifspec.IFSpecTags._
import textfixtures.JimpleDataflowFixture

import io.shiftleft.semanticcpg.language._

class TimeBomb extends JimpleDataflowFixture {

  behavior of
    """The method noLeak leaks the high value if the current date is in the past.
      |""".stripMargin

  override val code: String =
    """class Main {
      |  public static void main(String[] args) {
      |    int value = 5;
      |    noLeak(value);
      |  }
      |
      |  static long inThePast = 1456223086265L; // 23 Feb. 2016 11:24
      |
      |  public static int noLeak(int h) {
      |    long curr = System.currentTimeMillis();
      |    if (curr < inThePast) {
      |      return h;
      |    }
      |    return 0;
      |  }
      |}
      |
      |""".stripMargin

  "[True Negative] The requirement is, that the number passed to the 'noLeak' method" should
    " not be leaked." taggedAs(Library, ExplicitFlows) in {
    assertIsSecure(TaintSpec(
      cpg.call(".*noLeak.*").argument,
      cpg.method("noLeak").ast.isReturn
    ))
  }

}
