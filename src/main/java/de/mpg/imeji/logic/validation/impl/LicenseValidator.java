package de.mpg.imeji.logic.validation.impl;

import de.mpg.imeji.exceptions.UnprocessableError;
import de.mpg.imeji.logic.model.License;
import de.mpg.imeji.logic.validation.Validation;

/**
 * Validates license information, i.e. if url of license conforms to http-scheme
 * 
 * @author breddin
 *
 */
public class LicenseValidator extends ObjectValidator implements Validator<License> {


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
    if (!Validation.validateURLFormat(licenseURL)) {
      throw new UnprocessableError("error_full_web_url_license");
    }
  }

}
