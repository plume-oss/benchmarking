package com.github.plume.oss
package ifspec.aliasing

import textfixtures.JimpleDataflowFixture

import ifspec.IFSpecTags._

class AliasingNestedInsecure extends JimpleDataflowFixture {

  behavior of
    """The program creates an internal object, that has a field that contains another
      |object containing the secret value.
      |""".stripMargin

  override val code: String =
    """class Main {
      |
      |    static class A {
      |        B b;
      |
      |        A(B b) {
      |            this.b = b;
      |        }
      |    }
      |
      |    static class B {
      |        int val;
      |
      |        B(int val) {
      |            this.val = val;
      |        }
      |    }
      |
      |    static int secret = 42;
      |
      |    public static void main(String[] args) {
      |        B b = new B(1);
      |        A a = new A(b);
      |
      |        b.val = secret;
      |
      |        System.out.println(a.b.val);
      |    }
      |}
      |
      |""".stripMargin

  "[Insecure] The value stored in the field \"secret\" of class \"Main\"" should "be leaked via System.out.println()" taggedAs (Aliasing, ExplicitFlows) in {
    assertIsInsecure(specMainSecretLeakedToPrintln)
  }

}
