package com.github.plume.oss
package ifspec.simple

import ifspec.IFSpecTags._
import textfixtures.JimpleDataflowFixture

import io.shiftleft.semanticcpg.language._

class ObjectSensLeak extends JimpleDataflowFixture {

  behavior of
    """This program creates two objects of the same class, initializing their internal state with low and high values.
      |Afterwards, a method returning the object internal state is called on the object initialized with the low value.
      |""".stripMargin

  override val code: String =
    """class A {
      |	private int i;
      |
      |	public A(int i) {
      |		this.i = i;
      |	}
      |
      |	public int doPrint() {
      |		return out(this.i);
      |	}
      |
      |	public static int out(int i){
      |		return i;
      |	}
      |}
      |
      |class ObjectSensLeak {
      |
      |	public static int high = 0;
      |	public static int low = 1;
      |
      |	public static void main(String[] args) {
      |		test(high, low);
      |	}
      |
      |	public static int test(int h, int l) {
      |		A a1 = new A(l);
      |		A a2 = new A(h);
      |
      |		return a1.doPrint();
      |	}
      |
      |}
      |
      |""".stripMargin

  "[True Negative] The program" should "not leak the high value stored in ObjectSensLeak.high to System.out via A.out." +
    "It is however a valid operation to print the low value" taggedAs (Simple, ExplicitFlows) in {
    assertIsSecure(
      TaintSpec(
        cpg.method("main").call(".*test.*").argument,
        cpg.method("test").methodReturn
      )
    )
  }

}
