package de.mpg.imeji.logic.validation.impl;

import de.mpg.imeji.exceptions.UnprocessableError;

/**
 * Inteface for validators
 *
 * @author saquet
 *
 * @param <T>
 */
public interface Validator<T> {

  /**
   * Validate an object according the business rules
   *
   * @param t
   * @throws UnprocessableError
   */
  public void validate(T t, Method method) throws UnprocessableError;

  public enum Method {
    CREATE,
    UPDATE,
    DELETE,
    ALL;
  }
}
