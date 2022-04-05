package com.github.plume.oss
package ifspec.library

import ifspec.IFSpecTags._
import textfixtures.JimpleDataflowFixture

import io.shiftleft.semanticcpg.language._

class StringIntern extends JimpleDataflowFixture {

  behavior of
    """The program writes the secure value to the JVM string pool by invoking String.intern and can later check, on a
      |new string instance, whether String.intern was already invoked on an equal string.
      |""".stripMargin

  override val code: String =
    """class program {
      |  private static String a = "in";
      |  private static String b = "secure";
      |  private static String c = "i";
      |  private static String d = "nse";
      |  private static String e = "cure";
      |
      |  public static boolean foo(boolean h) {
      |    // Write h to the jvm string pool
      |    if (h)
      |      (a + b).intern();
      |
      |    // Read back the value of h from the string pool
      |    String s = (c + d + e);
      |    return (s.intern() != s);
      |  }
      |
      |  public static void main(String[] args) {
      |    foo(randBool());
      |  }
      |
      |  /** Helper method to obtain a random boolean */
      |  static boolean randBool() {
      |    return System.currentTimeMillis() % 2 == 0;
      |  }
      |
      |  /** Helper method to obtain a random integer */
      |  static int randInt() {
      |    return (int) System.currentTimeMillis();
      |  }
      |
      |}
      |
      |""".stripMargin

  "[Insecure] No information flow" should "occur between the input parameter h and the return value of the" +
    " method h." taggedAs(Library, ExplicitFlows) in {
    assertIsInsecure(TaintSpec(
      cpg.method("main").call(".*foo.*").argument,
      cpg.method("foo").methodReturn
    ))
  }

}
