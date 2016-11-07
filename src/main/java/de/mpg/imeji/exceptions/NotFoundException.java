package de.mpg.imeji.exceptions;


public class NotFoundException extends ImejiException {

  private static final long serialVersionUID = -6006945139992063194L;

  public NotFoundException(String message) {
    super(message, null);
    // minimizeStacktrace();
  }

}
