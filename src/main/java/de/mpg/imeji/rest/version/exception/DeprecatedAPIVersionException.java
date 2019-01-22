package de.mpg.imeji.rest.version.exception;

import de.mpg.imeji.exceptions.ImejiException;

/**
 * Exception when a request is done to the API with a deprecated API version
 *
 * @author saquet
 *
 */
public class DeprecatedAPIVersionException extends ImejiException {
  private static final long serialVersionUID = 3282632745855713344L;

  public DeprecatedAPIVersionException(String message) {
    super(message);
    this.minimizeStacktrace();
  }
}
