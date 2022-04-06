package com.github.plume.oss
package ifspec.highconditional

import ifspec.IFSpecTags._
import textfixtures.JimpleDataflowFixture

import io.shiftleft.semanticcpg.language._

class SimpleTypes extends JimpleDataflowFixture {

  behavior of
    """The program consists of four classes A, B, C and simpleTypes. Class B and C extend class A. The class simpleTypes
      |has a boolean field secret. In the main method of the class simpleTypes a variable object of type A is
      |initialized with a new instance of an object of class B or C where the concrete class is chosen depending on the
      |secret. Afterwards the value of an instanceof check with respect to the class B is printed to standard output.
      |""".stripMargin

  override val code: String =
    """
      |class A {
      |}
      |
      |class B extends A {
      |}
      |
      |class C extends A {
      |}
      |
      |class simpleTypes {
      |	static boolean secret = false;
      |
      |	public static void main(String[] args) {
      |		test();
      |	}
      |
      |	public static boolean test() {
      |		A obj;
      |
      |		if (secret) {
      |			obj = new B();
      |		} else {
      |			obj = new C();
      |		}
      |
      |		boolean reconstructed = obj instanceof B;
      |		return reconstructed;
      |	}
      |}
      |
      |""".stripMargin

  "[True Positive] The value of the field secret of the class simpleTypes" should "not leak to standard output. That is, " +
    "the value of the field secret of the class simpleTypes is the confidential information of the program and the " +
    "attacker is able to observe standard output." taggedAs(HighConditional, ImplicitFlows) in {
    assertIsInsecure(
      TaintSpec(
        cpg.fieldAccess.code(".*secret.*"),
        cpg.method("test").methodReturn
      )
    )
  }

}
