package com.github.plume.oss
package ifspec.highconditional

import ifspec.IFSpecTags._
import textfixtures.JimpleDataflowFixture

import io.shiftleft.semanticcpg.language._

class ScenarioPasswordSecure extends JimpleDataflowFixture {

  behavior of
    """The program abstracts from a login scenario in which a user has a fixed number of login tries before the program
      |blocks any further login attempts. After a successful login the user is allowed to request access to an URL.
      |The class PasswordManager of the program stores the password, the number of unsuccessful login attempts, the
      |maximal number of login attempts and whether the last login was successful or not. Moreover, it provides the
      |method tryLogin(String tryedPassword) and requestUrl(String url). The method checks first whether the number
      |of logins does not yet exceed the bounds of maximal logins. If this is the case it checks whether the given
      |password matches the password stored in this instance of the password manager adjusting the status and the
      |number of logins accordingly. Afterwards, the method prints the String "Login Attempt Completed" to the
      |standard output.
      |""".stripMargin

  override val code: String =
    """import java.io.BufferedReader;
      |import java.io.InputStreamReader;
      |
      |class Main {
      |	// just here to have an entry point for the program
      |	public static void main(String[] args) throws Exception {
      |		String exitKeyword = "exit";
      |		boolean exit = false;
      |
      |		PasswordManager pm = new PasswordManager();
      |
      |		System.out.println("To exit, type: " + exitKeyword);
      |
      |		while (!exit) {
      |			System.out.println("Enter password:");
      |			BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
      |			String input = br.readLine();
      |			exit |= input.equals(exitKeyword);
      |			pm.tryLogin(input);
      |
      |			System.out.println("Run completed, run again");
      |		}
      |	}
      |}
      |
      |class PasswordManager {
      |	private String password = "supersecret";
      |	private int invalidTries = 0;
      |	private int maximumTries = 10;
      |	private boolean loggedIn = false;
      |
      |	public void tryLogin(String tryedPassword) {
      |		if (this.invalidTries < this.maximumTries) {
      |			if (this.password.equals(tryedPassword)) {
      |				this.loggedIn = true;
      |			} else {
      |				this.loggedIn = false;
      |				this.invalidTries++;
      |			}
      |		}
      |		System.out.println("Login Attempt Completed");
      |	}
      |}
      |
      |""".stripMargin

  "[True Negative] The program" should "leak no information about the password to the user." taggedAs(HighConditional, ImplicitFlows) in {
    assertIsSecure(
      TaintSpec(
        cpg.fieldAccess.code(".*[password|loggedIn|invalidTries].*"),
        cpg.call(".*println.*").argument
      )
    )
  }

}
