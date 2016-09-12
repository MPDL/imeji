package de.mpg.imeji.logic.user.collaboration.email;

/**
 * An Email...
 *
 * @author bastiens
 *
 */
public final class Email {
  private final String body;
  private final String subject;

  public Email(String subject, String body) {
    this.subject = subject;
    this.body = body;
  }

  /**
   * @return the body
   */
  public String getBody() {
    return body;
  }

  /**
   * @return the subject
   */
  public String getSubject() {
    return subject;
  }
}
