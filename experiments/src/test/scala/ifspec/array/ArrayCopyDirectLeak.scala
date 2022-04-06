package com.github.plume.oss
package ifspec.array

import ifspec.IFSpecTags._
import textfixtures.JimpleDataflowFixture
import io.shiftleft.semanticcpg.language._

class ArrayCopyDirectLeak extends JimpleDataflowFixture {

  behavior of
    """The program generates h random numbers and assigns them to different indices in an array.
      |It then returns a sum of all the random numbers generated.
      |""".stripMargin

  override val code: String =
    """import java.util.Random;
      |
      |class Eg4 {
      |    public static int f(int h, int l, int[] a) {
      |        for (int i = 0; i < h; i++) {
      |            l += a[i];
      |        }
      |        return l;
      |    }
      |
      |    public static void main(String args[]) {
      |        int h = randInt();
      |        int l = randInt();
      |        int[] arr = randIntArray(h);
      |        f(h, l, arr);
      |    }
      |
      |    /** Helper method to obtain a random integer */
      |    static int randInt() {
      |        return 42;
      |    }
      |
      |    /**
      |     * Helper method to obtain an array of random
      |     * integers with the given length
      |     **/
      |    static int[] randIntArray(int length) {
      |        int[] ret = new int[length];
      |        for (int i = 0; i < length; i++) {
      |            ret[i] = randInt();
      |        }
      |        return ret;
      |    }
      |}
      |
      |""".stripMargin

  "[True Positive] The first parameter of 'f'" should "flow to the return value " +
    "of 'f'" taggedAs (Arrays, ExplicitFlows) in {
    assertIsInsecure(
      TaintSpec(
        cpg.method("main").call(".*f.*").argument(1),
        cpg.method("f").methodReturn
      )
    )
    assertIsInsecure(
      TaintSpec(
        cpg.method("main").call(".*f.*").argument(3),
        cpg.method("f").methodReturn
      )
    )
  }

}
