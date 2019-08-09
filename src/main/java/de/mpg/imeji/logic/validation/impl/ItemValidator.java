package de.mpg.imeji.logic.validation.impl;

import de.mpg.imeji.exceptions.UnprocessableError;
import de.mpg.imeji.logic.model.Item;
import de.mpg.imeji.logic.model.License;
import de.mpg.imeji.logic.model.Properties.Status;
import de.mpg.imeji.logic.model.util.LicenseUtil;

/**
 * {@link Validator} for an {@link Item}.
 *
 * @author saquet
 *
 */
public class ItemValidator extends ObjectValidator implements Validator<Item> {

  @Override
  public void validate(Item item, Method m) throws UnprocessableError {
    UnprocessableError error = new UnprocessableError();
    setValidateForMethod(m);
    if (isDelete()) {
      return;
    }
    // If a license and a license url is provided for an item, check
    // that the URL has a valid format (including http:// or https://) 
    LicenseValidator licenseValidator = new LicenseValidator();
    for (License license : item.getLicenses()) {
      licenseValidator.validate(license, m);
    }
    // Check that all published items have a license
    if (!item.getStatus().equals(Status.PENDING)) {
      final License lic = LicenseUtil.getActiveLicense(item);
      if (lic == null || lic.isEmtpy()) {
        error = new UnprocessableError("Items must have a license to be released", error);
      }
    }
    if (error.hasMessages()) {
      throw error;
    }
  }



}
