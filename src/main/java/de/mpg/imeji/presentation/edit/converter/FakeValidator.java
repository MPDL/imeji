package de.mpg.imeji.presentation.edit.converter;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.validator.FacesValidator;
import javax.faces.validator.Validator;
import javax.faces.validator.ValidatorException;

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
   * @see javax.faces.validator.Validator#validate(javax.faces.context.FacesContext,
   * javax.faces.component.UIComponent, java.lang.Object)
   */
  @Override
  public void validate(FacesContext arg0, UIComponent arg1, Object arg2) throws ValidatorException {
    // do nothing...
  }
}
