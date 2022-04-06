package com.github.plume.oss
package ifspec.simple

import ifspec.IFSpecTags._
import textfixtures.JimpleDataflowFixture

import io.shiftleft.semanticcpg.language._

class Polynomial extends JimpleDataflowFixture {

  behavior of
    """The method MyClass.compute computes p(h), where p(x) = x^6 + 3*x^4 + 3*x^2 + 1, and checks if p(h) is zero or not.
      |If it is zero, the method returns the high value h, if not, it returns the low value l.
      |Since p(x) = (x^2 + 1)^3, there does not exist value v such that p(v) = 0.
      |Therefore the method MyClass.compute does not leak the high value h.
      |(The program uses BigInteger; thus the arithmetic is precise and no overflow can happen and.)
      |""".stripMargin

  override val code: String =
    """
      |import java.util.*;
      |import java.lang.*;
      |import java.io.*;
      |import java.math.BigInteger;
      |
      |class MyClass {
      |	public static BigInteger compute(BigInteger h, BigInteger l) {
      |		/* x^6 + 3*x^4 + 3*x^2 + 1 ?= 0 */
      |		if ((h.pow(6).add(h.pow(4).multiply(BigInteger.valueOf(3))).add(h.pow(2).multiply(BigInteger.valueOf(3)))
      |				.add(BigInteger.valueOf(1))).compareTo(BigInteger.valueOf(0)) == 0) {
      |			return h;
      |		} else {
      |			return l;
      |		}
      |	}
      |
      |	public static void main(String[] args) throws java.lang.Exception {
      |		Random r = new Random(System.currentTimeMillis());
      |		BigInteger h = new BigInteger(32, r);
      |		BigInteger l = new BigInteger(32, r);
      |		System.out.println(h.toString());
      |		System.out.println(l.toString());
      |		BigInteger c = compute(h, l);
      |		System.out.println(c.toString());
      |	}
      |}
      |
      |""".stripMargin

  "[True Negative] The requirement is that the high value h passed to the 'MyClass.compute' method" should "not be " +
    "leaked by returning it" taggedAs (Simple, ImplicitFlows) in {
    assertIsSecure(
      TaintSpec(
        cpg.method("main").call(".*compute.*").argument,
        cpg.method("compute").methodReturn
      )
    )
  }

}
