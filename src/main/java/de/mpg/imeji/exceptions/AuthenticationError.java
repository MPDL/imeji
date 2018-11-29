package de.mpg.imeji.exceptions;

public class AuthenticationError extends ImejiException {
  private static final long serialVersionUID = 6128275500671470459L;

  public static final String USER_MUST_BE_LOGGED_IN = "Need to be logged-in to proceed with the operation.";

  /**
   * Constructor for HTTP 401 Unauthorized
   */
  public AuthenticationError(String message) {
    super(message);
    minimizeStacktrace();
  }

}
