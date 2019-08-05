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

  /**
   * Regular expression checks whether a given URL follows the https or https scheme. Goal is to
   * make sure that license URLs start with http:// or https:// so JSF can recognize them as
   * external links.
   * 
   * 1 Does it start with http or https -> (http|https) 2 Followed by :// -> :// 3 (optional)
   * user:password@ section with user consisting of word characters only and password consisting of
   * any sort of characters -> (\\w+:.+@)? 4 domain and sub domain section: for sub domain and
   * domain names we allow any characters except / \ . domain and sub domain names must be separated
   * by a . -> ([^/\\\\]+\\.)+[^\\./\\\\]+ 5 (optional) port :8080 (number is not limited) ->
   * (:\\d+)? 6 (optional) after a / any character allowed -> (/.*)?
   */

  private static String HTTP_SCHEME_REGEX = "(http|https)://(\\w+:.+@)?([^/\\\\]+\\.)+[^\\./\\\\]+(:\\d+)?(/.*)?";

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
