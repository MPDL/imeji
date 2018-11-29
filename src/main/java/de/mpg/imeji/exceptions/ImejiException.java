package de.mpg.imeji.exceptions;

import java.util.Arrays;

public class ImejiException extends Exception {
  private static final long serialVersionUID = -1024323233094119992L;

  public ImejiException() {
    super();
  }

  public ImejiException(String message) {
    super(message);
  }

  public ImejiException(String message, Throwable e) {
    super(message, e);
  }

  /**
   * When the Exception message is clear enough, we don't need the full stacktrace. This method
   * shows only the message plus the first element of the stacktrace
   */
  protected void minimizeStacktrace() {
    setStackTrace(Arrays.copyOf(getStackTrace(), 5));
  }

}
