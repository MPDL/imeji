package de.mpg.imeji.logic.validation.impl;

import java.util.regex.Pattern;

import de.mpg.imeji.exceptions.UnprocessableError;
import de.mpg.imeji.logic.model.License;

/**
 * Validates license information, i.e. if url of license conforms to http-scheme
 * 
 * @author breddin
 *
 */
public class LicenseValidator extends ObjectValidator implements Validator<License> {

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
  public void validate(License license, Method method) throws UnprocessableError {

    setValidateForMethod(method);
    if (isDelete()) {
      return;
    }

    if (license.getUrl() != null && !license.getUrl().isEmpty()) {
      this.checkLicenseURLFormat(license.getUrl());
    }

  }

  private void checkLicenseURLFormat(String licenseURL) throws UnprocessableError {
    if (!Pattern.matches(HTTP_SCHEME_REGEX, licenseURL)) {
      throw new UnprocessableError("Please specify full web URL for all licenses, i.e. http://www.myLicense.org");
    }
  }

}
