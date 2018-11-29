package de.mpg.imeji.exceptions;

public class TypeNotAllowedException extends Exception {

  private static final long serialVersionUID = 8649579301777694188L;

  public TypeNotAllowedException(String message) {
    super(message);
  }

  public TypeNotAllowedException(String message, Throwable e) {
    super(message, e);
  }

}
