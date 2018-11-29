package de.mpg.imeji.presentation.item.edit.converter;

import java.net.URI;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;

/**
 * {@link Converter} for {@link URI}
 *
 * @author saquet (initial creation)
 * @author $Author$ (last modification)
 * @version $Revision$ $LastChangedDate$
 */

public class URIConverter implements Converter {
  @Override
  public Object getAsObject(FacesContext arg0, UIComponent arg1, String arg2) {
    if (arg2 == null || "".equals(arg2.trim())) {
      return null;
    }
    arg2 = arg2.replaceAll("[^a-zA-Z0-9:/?#\\[\\]@!$&'()\\*\\+,;=\\-\\._~]", "-");

    URI uri = URI.create(arg2);

    if (!uri.isAbsolute()) {
      uri = URI.create("http://" + arg2);
    }
    return uri;
  }

  @Override
  public String getAsString(FacesContext arg0, UIComponent arg1, Object arg2) {
    if (arg2 == null) {
      return "";
    }
    return arg2.toString().trim();
  }
}
