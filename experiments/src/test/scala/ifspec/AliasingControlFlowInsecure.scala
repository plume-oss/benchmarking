package com.github.plume.oss
package ifspec

import textfixtures.JimpleDataflowFixture

import io.shiftleft.semanticcpg.language._

class AliasingControlFlowInsecure extends JimpleDataflowFixture {

  behavior of
    """The program creates two internal objects a and b that alias.
      |If the secret is equal to 42, the value of an internal field of a is set to 2 (otherwise it is 1).
      |Finally, the value of the same internal field of b is printed.
      |This leaks the fact whether the secret is 42 or not.
      |""".stripMargin

  override val code: String =
    """class Main {
      |
      |    static class A {
      |        int val;
      |
      |        A(int val) {
      |            this.val = val;
      |        }
      |    }
      |
      |    static private int secret = 42;
      |
      |    public static void main(String[] args) {
      |        A a = new A(1);
      |        A b = a;
      |
      |        if (secret == 42) {
      |            a.val = 2;
      |        }
      |
      |        System.out.println(b.val);
      |    }
      |}
      |
      |""".stripMargin

  "[Insecure] The value stored in the field \"secret\" of class \"Main\"" should "be leaked via System.out.println()" in {
    assertIsInsecure(specMainSecretLeakedToPrintln)
  }

}
