package de.mpg.imeji.exceptions;

/**
 * Exception for authentication of inactive users
 * 
 * @author bastiens
 *
 */
public class InactiveAuthenticationError extends AuthenticationError {
  private static final long serialVersionUID = 7976153766697083425L;

  public InactiveAuthenticationError(String message) {
    super(message);
  }
}
