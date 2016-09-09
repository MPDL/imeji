package de.mpg.imeji.j2j.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.net.URI;

/**
 * The identifier of a {@link j2jResource}. The id must be an {@link URI}
 *
 * @author saquet (initial creation)
 * @author $Author$ (last modification)
 * @version $Revision$ $LastChangedDate$
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface j2jId {
  public String setMethod();

  public String getMethod();
}
