package de.mpg.imeji.exceptions;



/**
 *
 */
public class NotAllowedError extends ImejiException {
  private static final long serialVersionUID = -3504946406047760565L;
  public static final String NOT_ALLOWED = "Not enough permissions to proceed with the operation.";

  /**
   * Constructor for HTTP 403 Forbidden
   */
  public NotAllowedError(String message) {
    super(message);
    minimizeStacktrace();
  }

}
