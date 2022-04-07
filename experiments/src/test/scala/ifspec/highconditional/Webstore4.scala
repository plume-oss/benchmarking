package com.github.plume.oss
package ifspec.highconditional

import ifspec.IFSpecTags._
import textfixtures.JimpleDataflowFixture

import io.shiftleft.semanticcpg.language._

class Webstore4 extends JimpleDataflowFixture {

  behavior of
    """A simple example for a video streaming service. The Store offers methods for watching either free previews of
      |prime value.
      |
      |The interesting method here is the reinit method. It replaces an object used for storing with a clone of that
      |object with the same contents. The actually stored videos are unchanged, but the object managing the storage of
      |the video actually is.
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
      |    w.reinit(true);
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
      |  public void reinit(boolean h) {
      |    if (h) {
      |      if (vids != null && vids.length > 0 && vids[0] != null) {
      |        VideoSet v = new VideoSet();
      |        v.vFree = vids[0].vFree;
      |        v.vPrime = vids[0].vPrime;
      |        vids[0] = v;
      |      }
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

  "[True Negative] There" should "be no information flow from the vPrime field of the videoset class to the output of the" +
    "seePreview method." taggedAs(HighConditional, ImplicitFlows) in {
    assertIsSecure(
      TaintSpec(
        cpg.fieldAccess.code(".*[vPrime|street].*"),
        cpg.method("seePreview").methodReturn
      )
    )
    assertIsSecure(
      TaintSpec(
        cpg.call(".*reinit.*").argument,
        cpg.method("seePreview").methodReturn
      )
    )
  }

}
