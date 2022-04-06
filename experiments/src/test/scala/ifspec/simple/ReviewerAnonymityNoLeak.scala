package com.github.plume.oss
package ifspec.simple

import ifspec.IFSpecTags._
import textfixtures.JimpleDataflowFixture

import io.shiftleft.semanticcpg.language._

class ReviewerAnonymityNoLeak extends JimpleDataflowFixture {

  behavior of
    """This example models a simplified peer review process. The class ReviewProcess
      |provides public functions to add reviews and to send out notifications to the
      |authors (here simply by printing the reviews on the standard output).
      |
      |The reviews are sorted before they are output.
      |This non-leaky version orders only based on review score and content.
      |In the leaky version (in a different directory), the sort order depends on the
      |reviewer identity.
      |""".stripMargin

  override val code: String =
    """import java.util.List;
      |import java.util.LinkedList;
      |import java.util.Collections;
      |
      |class ReviewProcess {
      |	private class Review implements Comparable<Review> {
      |		int reviewer_id;
      |		int score;
      |		String content;
      |
      |		public int compareTo(Review r) {
      |			if (this.reviewer_id != r.reviewer_id) {
      |				return (this.reviewer_id < r.reviewer_id) ? -1 : 1;
      |			} else if (this.score != r.score) {
      |				return (this.score < r.score) ? -1 : 1;
      |			} else {
      |				return this.content.compareTo(r.content);
      |			}
      |		}
      |	}
      |
      |	private List<Review> reviews;
      |
      |	ReviewProcess() {
      |		reviews = new LinkedList<Review>();
      |	}
      |
      |	public void addReview(int reviewer_id, int score, String content) {
      |		Review r = new Review();
      |		r.reviewer_id = reviewer_id;
      |		r.score = score;
      |		r.content = content;
      |		reviews.add(r);
      |	}
      |
      |	public void sendNotifications() {
      |		Collections.sort(reviews);
      |		for (Review r : reviews) {
      |			System.out.println("---");
      |			System.out.println("Score: " + r.score);
      |			System.out.println("Review: " + r.content);
      |			System.out.println("---");
      |		}
      |	}
      |
      |	public static void main(String args[]) {
      |		ReviewProcess rp = new ReviewProcess();
      |
      |		rp.addReview(42, 1, "Little novelty.");
      |		rp.addReview(5, 3, "Borderline paper.");
      |		rp.addReview(7, 4, "Significant contribution.");
      |
      |		rp.sendNotifications();
      |	}
      |}
      |
      |""".stripMargin

  "[True Negative] The reviewer identities" should "be kept secret. The observer is assumed to be an author, " +
    "receiving the reviews via the standard output of the program. Hence, this observable output must not depend on " +
    "the reviewer identities." taggedAs(Simple, ExplicitFlows) in {
    assertIsSecure(
      TaintSpec(
        cpg.method("main").call(".*addReview.*").argument,
        cpg.call("println").argument
      )
    )
  }

}
