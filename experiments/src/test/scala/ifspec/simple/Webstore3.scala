package com.github.plume.oss
package ifspec.simple

import ifspec.IFSpecTags._
import textfixtures.JimpleDataflowFixture

import io.shiftleft.semanticcpg.language._

class Webstore3 extends JimpleDataflowFixture {

  behavior of
    """The webstore manages two types of addresses, one for delivering and one for billing.
      |The specification is from the point of view of the delivery department.
      |They need the billing address, but not the delivery address for managing performing their task.
      |""".stripMargin

  override val code: String =
    """class Webstore {
      |  public int low;
      |  private int high;
      |
      |  private static int h;
      |  private static int l;
      |
      |  private int[] transaction;
      |
      |  public static void main(String[] args) {
      |
      |    Webstore w = new Webstore();
      |    w.setBillingAdr(l, l);
      |    w.setDeliveryAdr(h, h);
      |    w.getBillAdr();
      |    w.getDeliverAdr();
      |  }
      |
      |  private Address bill;
      |  private DAddress delivery;
      |
      |  public void setBillingAdr(int name, int street) {
      |    bill = new Address();
      |    bill.name = name;
      |    bill.street = street;
      |  }
      |
      |  public void setDeliveryAdr(int name, int street) {
      |    delivery = new DAddress();
      |    delivery.name = name;
      |    delivery.street = street;
      |  }
      |
      |  public int getBillAdr() {
      |    return this.bill.street;
      |  }
      |
      |  public int getDeliverAdr() {
      |    return this.delivery.street;
      |  }
      |
      |  public static class Address {
      |    public int name;
      |    public int street;
      |  }
      |
      |  public static class DAddress extends Address {
      |    public int name;
      |    public int street;
      |  }
      |}
      |
      |""".stripMargin

  "[True Negative] The high parameters which are given to the setDeliveryAddress method" should "not flow to the " +
    "billing address." taggedAs (Simple, ExplicitFlows) in {
    assertIsSecure(
      TaintSpec(
        cpg.call(".*setDeliveryAdr.*").argument,
        cpg.fieldAccess.code(".*[name|street].*")
      )
    )
  }

}
