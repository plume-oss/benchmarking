package com.github.plume.oss
package ifspec.classinitializer

import ifspec.IFSpecTags._
import textfixtures.JimpleDataflowFixture

import io.shiftleft.semanticcpg.language._

class StaticInitializersNoLeak extends JimpleDataflowFixture {

  behavior of
    """Static initializers technical example, ensuring that a class A is initialized before B.
      |""".stripMargin

  override val code: String =
    """
      |class Static_Initializers_NoLeak {
      |
      |	static String l = "Foo";
      |	static String h = "Top Secret";
      |	static String x = "Foo";
      |
      |	static class A {
      |		static int f = 17;
      |		static {
      |			l = x;
      |			System.out.println("Ainit");
      |		}
      |	}
      |
      |	static class B {
      |		static {
      |			x = h;
      |			System.out.println("Binit");
      |		}
      |	}
      |
      |	static void f(Object a, Object b) {
      |	}
      |
      |	public static void main(String[] args) {
      |		f(A.f, new B());
      |		/*
      |		 * int x = A.f;
      |		 * new B();
      |		 */
      |		System.out.println(l);
      |	}
      |
      |}
      |
      |""".stripMargin

  "[Secure] No information from the static field h" should "flow to the static " +
    "field l." taggedAs(ClassInitializer, ExplicitFlows) in {
    assertIsSecure(TaintSpec(
      cpg.fieldAccess.code(".*h.*"),
      cpg.fieldAccess.code(".*l.*")
    ))
  }

}
