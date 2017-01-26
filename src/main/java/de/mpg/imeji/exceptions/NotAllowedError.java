package de.mpg.imeji.exceptions;



/**
 *
 */
public class NotAllowedError extends ImejiException {
  private static final long serialVersionUID = -3504946406047760565L;

  /**
   * Constructor for HTTP 403 Forbidden
   */
  public NotAllowedError(String message) {
    super(message);
    minimizeStacktrace();
  }

}
