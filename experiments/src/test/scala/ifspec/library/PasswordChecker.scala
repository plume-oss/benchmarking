package com.github.plume.oss
package ifspec.library

import ifspec.IFSpecTags._
import textfixtures.JimpleDataflowFixture

import io.shiftleft.semanticcpg.language._

class PasswordChecker extends JimpleDataflowFixture {

  behavior of
    """Checks the strength of the entered password by evaluating the number of Caps
      |letters and alphanumeric characters in the password string. The result of the password check is then printed
      |to standard output.
      |""".stripMargin

  override val code: String =
    """import java.util.*;
      |import java.io.*;
      |import java.lang.*;
      |
      |class PasswordChecker {
      |	static int countAlphanumeric(String passwd) {
      |		int count = 0;
      |		for (int i = 0; i < passwd.length(); i++)
      |			if (Character.isDigit(passwd.charAt(i)) || Character.isLetter(passwd.charAt(i)))
      |				count++;
      |
      |		return count;
      |	}
      |
      |	static int countCaps(String passwd) {
      |		int count = 0;
      |		for (int i = 0; i < passwd.length(); i++)
      |			if (passwd.charAt(i) >= 'A' && passwd.charAt(i) <= 'Z')
      |				count++;
      |		return count;
      |	}
      |
      |	static int passwordstrength(String passwd) {
      |		int strength = 0;
      |
      |		if (countAlphanumeric(passwd) < 3)
      |			strength = 1;
      |
      |		if (countCaps(passwd) < 3)
      |			strength = 2;
      |		else
      |			strength = 3;
      |
      |		return strength;
      |	}
      |
      |	public static void main(String args[]) {
      |		Scanner input = new Scanner(System.in);
      |		String passwd = input.nextLine();
      |		int strength = passwordstrength(passwd);
      |		System.out.println(strength);
      |	}
      |}
      |
      |""".stripMargin

  "[True Positive] No information about the provided password" should "leak to the program output" taggedAs (Library, ImplicitFlows) in {
    assertIsInsecure(
      TaintSpec(
        cpg.call(".*nextLine.*").cfgPrev,
        cpg.call(".*println.*").argument(0)
      )
    )
  }

}
