package com.github.plume.oss
package ifspec.simple

import ifspec.IFSpecTags._
import textfixtures.JimpleDataflowFixture

import io.shiftleft.semanticcpg.language._

class StaticDispatching extends JimpleDataflowFixture {

  behavior of
    """If the low variable is equal to 1 the method set(long) is called which outputs the high value to the low sink.
      |Otherwise the method set(int) is called which outputs the high value to the high sink.
      |""".stripMargin

  override val code: String =
    """class StaticDispatching {
      |
      |	int h, l;
      |
      |	int lsink, hsink;
      |
      |	public void f() {
      |		if (l == 1)
      |			set((long) h);
      |		else
      |			set(h);
      |	}
      |
      |	public void set(long a) {
      |		lsink = (int) a;
      |	}
      |
      |	public void set(int a) {
      |		hsink = a;
      |	}
      |
      |	public static void main(String[] args) {
      |		StaticDispatching sd = new StaticDispatching();
      |		sd.f();
      |	}
      |}
      |
      |""".stripMargin

  "[Insecure] The value of field h is" should "not be written to the field lsink." taggedAs(Simple, ExplicitFlows) in {
    assertIsInsecure(TaintSpec(
      cpg.fieldAccess.code(".*[h|l].*"),
      cpg.fieldAccess.code(".*[hsink|lsink].*")
    ))
  }

}
