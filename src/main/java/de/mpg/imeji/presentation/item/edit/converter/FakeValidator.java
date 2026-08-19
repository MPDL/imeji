package de.mpg.imeji.presentation.item.edit.converter;

import jakarta.faces.component.UIComponent;
import jakarta.faces.context.FacesContext;
import jakarta.faces.validator.FacesValidator;
import jakarta.faces.validator.Validator;
import jakarta.faces.validator.ValidatorException;

/***
 *
 * @author saquet (initial creation)
 * @author $Author$ (last modification)
 * @version $Revision$ $LastChangedDate$
 */
@FacesValidator("FakeValidator")
public class FakeValidator implements Validator {
  /*
   * (non-Javadoc)
   *
   * @see
   * jakarta.faces.validator.Validator#validate(jakarta.faces.context.FacesContext,
   * jakarta.faces.component.UIComponent, java.lang.Object)
   */
  @Override
  public void validate(FacesContext arg0, UIComponent arg1, Object arg2) throws ValidatorException {
    // do nothing...
  }
}
