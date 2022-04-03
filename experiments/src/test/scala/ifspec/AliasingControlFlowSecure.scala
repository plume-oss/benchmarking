package com.github.plume.oss
package ifspec

import textfixtures.JimpleDataflowFixture

import io.joern.dataflowengineoss.language._
import io.shiftleft.semanticcpg.language._

class AliasingControlFlowSecure extends JimpleDataflowFixture {

  behavior of "a program that creates two internal objects a and b that alias. (secure)"

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
      |        } else {
      |            a.val = secret;
      |        }
      |
      |        System.out.println(b.val);
      |    }
      |}
      |
      |""".stripMargin

  "The value stored in the field \"secret\" of class \"Main\"" should "be not be leaked via System.out.println()" in {
    val source = cpg.fieldAccess.code("Main.secret")
    val sink = cpg.method("main").call(".*println.*")
    sink.reachableBy(source).size shouldBe 0
  }

}
