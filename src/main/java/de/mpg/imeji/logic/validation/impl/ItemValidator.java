package de.mpg.imeji.logic.validation.impl;

import java.util.regex.Pattern;

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

  private static String HTTP_SCHEME_REGEX = "(http|https)://(\\w+:\\w+@)?([^/\\\\]+\\.)+[^\\./\\\\]+(:\\d+)?(/.*)?";

  @Override
  public void validate(Item item, Method m) throws UnprocessableError {
    UnprocessableError error = new UnprocessableError();
    setValidateForMethod(m);
    if (isDelete()) {
      return;
    }
    // If a license and a license url is provided for an item, check
    // that the URL has a valid format (including http:// or https://) 
    for (License license : item.getLicenses()) {
      if (license.getUrl() != null && !license.getUrl().isEmpty()) {
        this.checkLicenseURLFormat(license.getUrl());
      }
    }
    // Check that all publish items have a license
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


  private void checkLicenseURLFormat(String licenseURL) throws UnprocessableError {
    if (!Pattern.matches(HTTP_SCHEME_REGEX, licenseURL)) {
      throw new UnprocessableError("Please specify full web URL for all licenses, i.e. http://www.myLicense.org");
    }
  }
}
