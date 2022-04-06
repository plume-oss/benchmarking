package com.github.plume.oss
package ifspec.highconditional

import ifspec.IFSpecTags._
import textfixtures.JimpleDataflowFixture

import io.shiftleft.semanticcpg.language._

class ScenarioBankingSecure extends JimpleDataflowFixture {

  behavior of
    """This example describes a banking scenario. The AccountOwner has an Account and can pay an amount to a beneficiary.
      |The successfully completed transactions are logged in a TransactionLog, while errors are written down in an
      |ErrorLog.
      |""".stripMargin

  override val code: String =
    """class Account {
      |
      |    double balance;
      |
      |    ErrorLog errorLog = new ErrorLog();
      |
      |    TransactionLog transactionLog = new TransactionLog();
      |
      |    public void deposit(double amount) {
      |        if (amount > 0) {
      |            this.balance += amount;
      |            this.logTransaction(true);
      |        } else {
      |            this.logError("Cannot deposit a non-positive amount.");
      |        }
      |    }
      |
      |    public boolean withdraw(double amount) {
      |        if (amount > 0) {
      |            double newAmount = this.balance - amount;
      |            if (newAmount > 0) {
      |                this.balance = newAmount;
      |                this.logTransaction(false);
      |                return true;
      |            } else {
      |                return false;
      |            }
      |        }
      |        this.logError("Cannot withdraw a non-positive amount.");
      |        return false;
      |    }
      |
      |    private void logTransaction(boolean isDeposit) {
      |        String transaction = isDeposit ? "Deposit" : "Withdrawal";
      |        this.transactionLog.logTransaction(transaction + " completed, new balance: " + this.balance);
      |    }
      |
      |    public void logError(String message) {
      |        this.errorLog.logError(message);
      |    }
      |
      |}
      |
      |class AccountOwner {
      |    private Account account;
      |
      |    public AccountOwner(Account account) {
      |        this.account = account;
      |    }
      |
      |    public void payBeneficiary(Beneficiary b, double amount) {
      |        boolean transactionPossible = this.account.withdraw(amount);
      |        if (transactionPossible) {
      |            b.receive(amount);
      |        }
      |    }
      |
      |}
      |
      |class Beneficiary {
      |
      |	private double received;
      |
      |	public void receive(double amount) {
      |		this.received += amount;
      |	}
      |}
      |
      |class ErrorLog {
      |
      |	public void logError(String message) {
      |		System.out.println(message);
      |	}
      |}
      |
      |class Main {
      |    public static void main(String[] args) {
      |        Account account = new Account();
      |        account.deposit(100);
      |        AccountOwner owner = new AccountOwner(account);
      |        Beneficiary beneficiary = new Beneficiary();
      |        owner.payBeneficiary(beneficiary, 150);
      |    }
      |}
      |
      |class TransactionLog {
      |
      |    public void logTransaction(String message) {
      |    }
      |
      |}
      |
      |""".stripMargin

  "[True Negative] The requirement is to be disallow information flows from Account.balance to ErrorLog.logError. " +
    "That is, no information about account " +
    "balances" should "be stored in the error log." taggedAs(HighConditional, ImplicitFlows) in {
    assertIsSecure(
      TaintSpec(
        cpg.fieldAccess.code(".*balance.*"),
        cpg.call(".*log.*").argument
      )
    )
  }

}
