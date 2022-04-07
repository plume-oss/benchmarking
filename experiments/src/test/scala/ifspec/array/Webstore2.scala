package com.github.plume.oss
package ifspec.array

import ifspec.IFSpecTags._
import textfixtures.JimpleDataflowFixture

import io.shiftleft.semanticcpg.language._

class Webstore2 extends JimpleDataflowFixture {

  behavior of
    """A simple example for a video streaming service. The Store offers methods for watching either free previews of
      |prime value.
      |""".stripMargin

  override val code: String =
    """class Webstore {
      |  public int low;
      |  private int high;
      |
      |  private static int h;
      |  private static int l;
      |
      |  private int[] transaction;
      |
      |  public static void main(String[] args) {
      |
      |    Webstore w = new Webstore();
      |    w.seePreview(l);
      |    w.seePrime(l);
      |  }
      |
      |  private VideoSet[] vids;
      |
      |  public Video seePreview(int i) {
      |    if (vids != null && 0 <= i && i < vids.length) {
      |      return vids[i].vFree;
      |    } else {
      |      return null;
      |    }
      |  }
      |
      |  public Video seePrime(int i) {
      |    if (vids != null && 0 <= i && i < vids.length) {
      |      return vids[i].vPrime;
      |    } else {
      |      return null;
      |    }
      |  }
      |
      |  public static class VideoSet {
      |    public Video vFree;
      |    public Video vPrime;
      |  }
      |
      |  public static class Video {
      |  }
      |}
      |
      |""".stripMargin

  "[True Negative] There" should "be no flow from the field vPrime to the return value of " +
    "seePreview." taggedAs(Arrays, ExplicitFlows) in {
    assertIsSecure(TaintSpec(
      cpg.fieldAccess.code(".*vPrime.*"),
      cpg.method("seePreview").ast.isReturn
    ))
  }

}
