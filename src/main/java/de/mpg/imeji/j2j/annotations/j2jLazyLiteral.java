package de.mpg.imeji.j2j.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * A Literal which is not loaded in lazy loading
 * 
 * @author saquet
 *
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface j2jLazyLiteral {
  public String value();
}
