package com.github.plume.oss
package ifspec.classinitializer

import ifspec.IFSpecTags._
import textfixtures.JimpleDataflowFixture

import io.shiftleft.semanticcpg.language._

class StaticInitializersLeak extends JimpleDataflowFixture {

  behavior of
    """Static initializers which copy the value of h to l, via x.
      |""".stripMargin

  override val code: String =
    """
      |class Static_Initializers_Leak {
      |
      |	static String l = "Foo";
      |	static String h = "Top Secret";
      |
      |	static String x = "Foo";
      |	static {
      |		x = h;
      |	}
      |
      |	public static void main(String[] args) {
      |		l = x;
      |		System.out.println(l);
      |	}
      |
      |}
      |
      |""".stripMargin

  "[Insecure] No information from the static field h" should "flow to the static " +
    "field l." taggedAs(ClassInitializer, ExplicitFlows) in {
    assertIsInsecure(TaintSpec(
      cpg.fieldAccess.code(".*h.*"),
      cpg.fieldAccess.code(".*l.*")
    ))
  }

}
