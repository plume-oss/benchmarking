package com.github.plume.oss
package ifspec.exceptions

import ifspec.IFSpecTags._
import textfixtures.JimpleDataflowFixture
import io.shiftleft.semanticcpg.language._

class ExceptionDivZero extends JimpleDataflowFixture {

  behavior of
    """The program asks the user to input two private integers and outputs the integer quotient of the two numbers.
      |Additionally, the program saves the query to analyze them later.
      |The guarantee that the query will not be saved publicly is given by the program.
      |If an error occurs, the error is added to a public log file on the disk, such that a technician can solve the problem.
      |""".stripMargin

  override val code: String =
    """import java.util.Scanner;
      |
      |class ExceptionDivZero {
      |	public ExceptionDivZero() {
      |		Scanner sc = new Scanner(System.in);
      |		System.out.println("Please enter two private integers a and b.");
      |		System.out.println("We will return a/b in integer precision");
      |		System.out.print("Enter one private integer: ");
      |		int i1 = sc.nextInt();
      |		System.out.print("Enter an other private integer: ");
      |		int i2 = sc.nextInt();
      |		int r = i1/i2;
      |		String result = i1 + " / " + i2 + " = " + r;
      |		System.out.println(" " + result);
      |		System.out.println("Your query was saved in our secure database,"
      |				+ " such that we can use it for marketing issues.");
      |		System.out.println("We guarantee that your query will not be saved publicly on the disk.");
      |		this.writeToDB(result);
      |		sc.close();
      |	}
      |
      |	private void writeToDB(String entry) {
      |		// saves the new entry to a secure DB for logging of user inputs
      |		System.out.println("writeToDB:" + entry);
      |	}
      |
      |	public static void writeToDisk(String err) {
      |		// saves the error into a public log file, such that technicians can solve the problem.
      |		System.out.println("writeToDisk:" + err);
      |	}
      |
      |	public static void main(String... args) {
      |		try	{
      |			new ExceptionDivZero();
      |		} catch (Exception e) {
      |			writeToDisk(e.toString());
      |		}
      |	}
      |}
      |
      |""".stripMargin

  "[Insecure] The integers input by the users" should "not be saved on the disk. " +
    "This means that no flow of information from the return value of Scanner.nextInt() " +
    "to the parameter of the method writeToDisk() should occur" taggedAs (Exceptions, ImplicitFlows) in {
    assertIsInsecure(
      TaintSpec(
        cpg.call(".*nextInt.*").astParent.astChildren.isIdentifier,
        cpg.call.methodFullName(".*writeToDisk.*", ".*writeToDB.*").argument(1),
      )
    )
  }

}
